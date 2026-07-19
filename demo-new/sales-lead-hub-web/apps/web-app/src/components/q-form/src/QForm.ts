import { defineComponent, ref, computed, unref, nextTick, h, resolveComponent } from 'vue'
import type { PropType } from 'vue'
import type { FormSchema, FormLayout, FormMode } from './types'
import QFormItem from './QFormItem'

export default defineComponent({
  name: 'QForm',
  components: { QFormItem },
  inheritAttrs: false,

  props: {
    model: { type: Object as PropType<Record<string, any>>, default: () => ({}) },
    schemas: { type: Array as PropType<FormSchema[]>, default: () => [] },
    layout: { type: String as PropType<FormLayout>, default: 'vertical' },
    labelWidth: { type: [String, Number] as PropType<string | number>, default: '' },
    colon: { type: Boolean, default: true },
    rowProps: { type: Object, default: () => ({ gutter: [16, 0] }) },
    disabled: { type: Boolean, default: false },
    mode: { type: String as PropType<FormMode>, default: 'edit' },
    resetKeepRender: { type: Boolean, default: false }
  },

  emits: ['update:model', 'submit', 'reset', 'finish', 'finish-failed', 'form-event'],

  setup(props, { emit, expose, slots }) {
    const formRef = ref<any>(null)
    const formItemRefs = ref<Record<string, any>>({})

    const renderKey = computed(() => `q-form-${props.mode}`)

    const visibleSchemas = computed(() => {
      return props.schemas.filter(schema => {
        const { show } = schema
        if (show === undefined) return true
        if (typeof show === 'boolean') return show
        if (typeof show === 'function') return show({ model: props.model, field: schema.field })
        return true
      })
    })

    const getLabelCol = computed(() => {
      const w = unref(props.labelWidth)
      if (!w) return undefined
      return { style: { width: typeof w === 'number' ? `${w}px` : w } }
    })

    const formProps = computed(() => ({
      layout: props.layout,
      labelWidth: props.labelWidth,
      disabled: props.disabled
    }))

    function setFormModel(field: string, value: any) {
      emit('update:model', { ...props.model, [field]: value })
      nextTick(() => {
        formRef.value?.validateFields([field]).catch(() => {})
      })
    }

    function setFormItemRef(key: string | number, el: any) {
      if (el) formItemRefs.value[key] = el
    }

    function getModel() {
      return props.model
    }

    async function validate() {
      return formRef.value?.validate()
    }

    async function validateFields(nameList?: string[]) {
      return formRef.value?.validateFields(nameList)
    }

    function resetFields() {
      formRef.value?.resetFields()
      Object.values(formItemRefs.value || {}).forEach((item: any) => {
        const target = item?.proxy || item
        if (target && typeof target.resetCustomRender === 'function') {
          target.resetCustomRender()
        }
      })
      emit('reset')
    }

    function clearValidate(name?: string | string[]) {
      formRef.value?.clearValidate(name)
    }

    async function submit() {
      try {
        const values = await validate()
        emit('finish', values)
        emit('submit', values)
      } catch (errorInfo) {
        emit('finish-failed', errorInfo)
        nextTick(() => {
          document
            .querySelector('.ant-form-item-has-error')
            ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
        })
      }
    }

    expose({
      getModel,
      validate,
      validateFields,
      resetFields,
      clearValidate,
      submit,
      setFormModel,
      get formRef() { return formRef.value }
    })

    function handleFinish(values: any) {
      emit('finish', values)
    }

    function handleFinishFailed(errorInfo: any) {
      emit('finish-failed', errorInfo)
      nextTick(() => {
        document
          .querySelector('.ant-form-item-has-error')
          ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      })
    }

    return () => {
      const AForm = resolveComponent('AForm')
      const ARow = resolveComponent('ARow')
      const ACol = resolveComponent('ACol')

      const schemaList = visibleSchemas.value

      const cols = schemaList.map((schema, index) => {
        const key = `q-form-item-${schema.field || index}`

        const formItem = h(QFormItem, {
          ref: (el: any) => setFormItemRef(schema.field || index, el),
          schema,
          formModel: props.model,
          formProps: formProps.value,
          disabled: props.disabled,
          mode: props.mode,
          setFormModel,
          onFormEvent: (e: any) => emit('form-event', e)
        }, slots)

        if (props.layout === 'inline') {
          return h('span', { key, style: 'display:inline-block; vertical-align:top' }, [formItem])
        }

        const colProps = schema.colProps || { span: 24 }
        return h(ACol, { key, ...colProps }, () => formItem)
      })

      return h(
        AForm,
        {
          key: renderKey.value,
          ref: formRef,
          class: [
            'q-form',
            props.disabled ? 'is-disabled' : '',
            props.mode === 'view' ? 'only-view' : ''
          ],
          model: props.model,
          layout: props.layout,
          labelCol: getLabelCol.value,
          colon: props.colon,
          disabled: props.disabled,
          scrollToFirstError: true,
          onFinish: handleFinish,
          onFinishFailed: handleFinishFailed
        },
        () => props.layout === 'inline' ? cols : h(ARow, props.rowProps, () => cols)
      )
    }
  }
})
