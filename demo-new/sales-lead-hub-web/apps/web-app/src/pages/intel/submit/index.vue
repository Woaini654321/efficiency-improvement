<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto flex flex-col">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-bold">{{ t('intel.submit') }}</h2>
      <a-button @click="router.back()">{{ t('common.back') }}</a-button>
    </div>

    <a-alert type="info" show-icon class="mb-3" :message="t('intel.submitTip')" />

    <div class="flex-1">
      <QForm ref="qFormRef" :schemas="schemas" v-model:model="formModel" />
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-[hsl(var(--line))] mt-4">
      <a-button @click="router.back()">{{ t('common.cancel') }}</a-button>
      <a-button type="primary" @click="handleSubmit">{{ t('intel.submitAction') }}</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { QForm } from '@/components/q-form'
import { submitCompetitorIntel } from '@/apis/intel/intelApi'

defineOptions({ name: 'IntelSubmit' })
definePage({
  name: 'IntelSubmit',
  meta: {
    layout: false,
    menu: false,
    title: 'intel.submit'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const qFormRef = ref()

const formModel = reactive<{
  brand: string
  product: string
  intelType: string
  source: string
  title: string
  content: string
}>({
  brand: '',
  product: '',
  intelType: '',
  source: '',
  title: '',
  content: ''
})

const brandOptions = computed(() =>
  ['Sierra Wireless', 'Telit', '广和通', '移远', 'u-blox', 'Thales'].map((b) => ({ label: b, value: b }))
)
const intelTypeOptions = computed(() => [
  { label: t('dict.intelType.new_product'), value: 'new_product' },
  { label: t('dict.intelType.price_change'), value: 'price_change' },
  { label: t('dict.intelType.customer_case'), value: 'customer_case' },
  { label: t('dict.intelType.other'), value: 'other' }
])

const schemas = computed(() => [
  {
    field: 'brand',
    label: t('intel.brand'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), showSearch: true, options: brandOptions.value, style: 'width:100%' }
  },
  {
    field: 'product',
    label: t('intel.product'),
    component: 'Input',
    componentProps: { placeholder: t('common.inputPlaceholder'), maxlength: 50 }
  },
  {
    field: 'intelType',
    label: t('intel.intelType'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), options: intelTypeOptions.value, style: 'width:100%' }
  },
  {
    field: 'source',
    label: t('intel.source'),
    component: 'Input',
    rules: [{ required: true, message: t('intel.sourcePlaceholder') }],
    componentProps: { placeholder: t('intel.sourcePlaceholder'), maxlength: 100 }
  },
  {
    field: 'title',
    label: t('intel.title'),
    component: 'Input',
    rules: [{ required: true, message: t('intel.titlePlaceholder') }],
    componentProps: { placeholder: t('intel.titlePlaceholder'), maxlength: 100, showCount: true }
  },
  {
    field: 'content',
    label: t('intel.content'),
    component: 'Textarea',
    rules: [{ required: true, message: t('intel.contentPlaceholder') }],
    componentProps: { placeholder: t('intel.contentPlaceholder'), rows: 8, maxlength: 5000, showCount: true }
  }
])

async function handleSubmit() {
  await qFormRef.value?.validate()
  await submitCompetitorIntel({ ...formModel })
  message.success(t('intel.submitSuccess'))
  router.push({ path: '/intel' })
}
</script>
