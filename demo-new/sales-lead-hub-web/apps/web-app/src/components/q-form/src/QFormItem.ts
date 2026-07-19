import { defineComponent, computed, ref, resolveComponent as vueResolveComponent, h } from 'vue'
import type { PropType } from 'vue'
import type { FormSchema, FormMode, SetFormModel } from './types'
import { isFunction, resolveComponent } from './utils'

export default defineComponent({
  name: 'QFormItem',

  props: {
    schema: { type: Object as PropType<FormSchema>, required: true },
    formModel: { type: Object as PropType<Record<string, any>>, required: true },
    formProps: { type: Object as PropType<Record<string, any>>, default: () => ({}) },
    setFormModel: { type: Function as PropType<SetFormModel>, required: true },
    disabled: { type: Boolean, default: false },
    mode: { type: String as PropType<FormMode>, default: 'edit' }
  },

  emits: ['form-event'],

  setup(props, { emit, slots, expose }) {
    const customRenderRef = ref<any>(null)

    const resolvedComp = computed(() => {
      const { component } = props.schema
      if (!component || isFunction(component)) return null
      return resolveComponent(component as string | object)
    })

    const valuePropName = computed(() => props.schema.valuePropName || 'value')
    const updateEventName = computed(() => props.schema.updateEventName || 'update:value')

    const getComponentProps = computed(() => {
      const { componentProps = {}, field, on, component } = props.schema

      let resolvedProps: Record<string, any> = isFunction(componentProps)
        ? (componentProps as Function)({ model: props.formModel, field })
        : { ...componentProps }

      const { slotsRender, ...restProps } = resolvedProps
      resolvedProps = restProps

      if (['Input', 'input'].includes(component as string)) {
        resolvedProps = { maxlength: 200, ...resolvedProps }
      }
      if (['Textarea', 'textarea'].includes(component as string)) {
        resolvedProps = { maxlength: 500, showCount: true, ...resolvedProps }
      }

      if (on) {
        for (const [event, handler] of Object.entries(on)) {
          const emitKey = `on${event.charAt(0).toUpperCase()}${event.slice(1)}`
          resolvedProps[emitKey] = (...args: any[]) => {
            if (isFunction(handler)) handler(props.formModel, ...args)
            emit('form-event', {
              event,
              value: args[0],
              args,
              field: props.schema.field,
              schema: props.schema
            })
          }
        }
      }

      return resolvedProps
    })

    const getComponentSlots = computed(() => {
      const { componentProps = {} } = props.schema
      const resolved = isFunction(componentProps)
        ? (componentProps as Function)({ model: props.formModel, field: props.schema.field })
        : componentProps
      return (resolved as Record<string, any>).slotsRender || {}
    })

    const getRules = computed(() => {
      const { rules, required, dynamicRules, component, label } = props.schema
      const isText = ['Input', 'Textarea', 'InputPassword'].includes(component as string)

      if (isFunction(dynamicRules)) {
        return dynamicRules({ model: props.formModel, field: props.schema.field })
      }

      const rulesArr = [...(rules || [])]
      rulesArr.forEach((rule: any) => {
        if (rule.required && !rule.message && !rule.validator) {
          rule.message = `${label}不能为空`
        }
        if (isText) rule.whitespace = true
        else delete rule.whitespace
      })

      if (required && !rulesArr.some((r: any) => r.required)) {
        rulesArr.unshift({
          required: true,
          message: `${label}不能为空`,
          ...(isText ? { whitespace: true } : {})
        })
      }

      return rulesArr
    })

    const getRequired = computed(() => {
      const { required, rules = [] } = props.schema
      return required || rules.some((r: any) => r.required)
    })

    const getLabelCol = computed(() => props.schema.labelCol || props.formProps.labelCol)
    const getWrapperCol = computed(() => props.schema.wrapperCol || props.formProps.wrapperCol)

    function updateValue(val: any) {
      const { field, trim } = props.schema
      if (trim && typeof val === 'string') val = val.trim()
      props.setFormModel(field, val)
    }

    expose({
      resetCustomRender() {
        const inst = customRenderRef.value
        const target = inst?.proxy || inst
        if (!target) return
        ;['resetCountdown', 'reset', 'resetFields'].forEach(method => {
          if (isFunction(target[method])) target[method]()
        })
      }
    })

    function renderFieldContent() {
      const { schema, formModel, disabled, mode } = props
      const { render, slot } = schema

      if (isFunction(render)) {
        return h(
          { render: () => render!(formModel, props.setFormModel, schema) },
          { ref: customRenderRef }
        )
      }

      if (slot) {
        return slots[slot]?.({ model: formModel, field: schema.field, values: formModel })
      }

      const comp = resolvedComp.value
      if (!comp) return null

      const actualComp = mode === 'view' && schema.viewRender ? schema.viewRender : comp

      const vpn = valuePropName.value
      const uen = updateEventName.value

      const bindProps: Record<string, any> = {
        ...getComponentProps.value,
        [vpn]: formModel[schema.field],
        disabled: mode === 'view' ? true : disabled,
        ...(schema.noUpdateTrigger ? {} : { [uen]: updateValue, 'onUpdate:value': updateValue })
      }

      const compSlots = getComponentSlots.value

      return h(
        typeof actualComp === 'string' ? vueResolveComponent(actualComp) : actualComp as any,
        bindProps,
        Object.keys(compSlots).length ? compSlots : undefined
      )
    }

    return () => {
      const { schema } = props
      const AFormItem = vueResolveComponent('AFormItem')
      return h(
        AFormItem,
        {
          class: schema.class,
          label: schema.label,
          name: schema.field,
          rules: getRules.value,
          required: getRequired.value,
          labelCol: getLabelCol.value,
          wrapperCol: getWrapperCol.value,
          extra: schema.extra,
          noStyle: schema.noStyle
        },
        {
          ...(schema.labelRender || schema.labelPlaceholder
            ? {
                label: () =>
                  schema.labelRender
                    ? h(schema.labelRender)
                    : h('span', { style: 'visibility:hidden' }, 'Label')
              }
            : {}),
          default: () => renderFieldContent()
        }
      )
    }
  }
})
