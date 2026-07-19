<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto flex flex-col">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-bold">{{ isEdit ? t('opportunity.editTitle') : t('opportunity.add') }}</h2>
      <a-button @click="router.back()">{{ t('common.back') }}</a-button>
    </div>

    <a-alert v-if="isCopy" type="warning" show-icon class="mb-3" :message="t('opportunity.copyTip')" />

    <div class="flex-1">
      <QForm ref="qFormRef" :schemas="schemas" v-model:model="formModel" />
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-[hsl(var(--line))] mt-4">
      <a-button @click="handleSave('draft')">{{ t('common.saveDraft') }}</a-button>
      <a-button type="primary" @click="handleSave('published')">{{ t('opportunity.publish') }}</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { QForm } from '@/components/q-form'
import { getOpportunityDetail, createOpportunity, updateOpportunity } from '@/apis/opportunity/opportunityApi'

defineOptions({ name: 'OpportunityForm' })
definePage({
  name: 'OpportunityForm',
  meta: {
    layout: false,
    menu: false,
    title: 'opportunity.form'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const qFormRef = ref()

const editId = route.query.id as string | undefined
const copyId = route.query.copyFrom as string | undefined
const isEdit = computed(() => !!editId)
const isCopy = computed(() => !!copyId)

const formModel = reactive<{
  title: string
  type: string
  categoryIds: string[]
  industry: string
  keywords: string[]
  summary: string
  content: string
}>({
  title: '',
  type: '',
  categoryIds: [],
  industry: '',
  keywords: [],
  summary: '',
  content: ''
})

const typeOptions = computed(() => [
  { label: t('dict.oppType.product_info'), value: 'product_info' },
  { label: t('dict.oppType.solution'), value: 'solution' },
  { label: t('dict.oppType.success_case'), value: 'success_case' }
])

const schemas = computed(() => [
  {
    field: 'title',
    label: t('opportunity.title'),
    component: 'Input',
    rules: [{ required: true, message: t('opportunity.titlePlaceholder') }],
    componentProps: { placeholder: t('opportunity.titlePlaceholder'), maxlength: 100, showCount: true }
  },
  {
    field: 'type',
    label: t('opportunity.type'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), options: typeOptions.value, style: 'width:100%' }
  },
  {
    field: 'categoryIds',
    label: t('opportunity.category'),
    component: 'Select',
    componentProps: { placeholder: t('opportunity.categoryPlaceholder'), mode: 'tags', style: 'width:100%' }
  },
  {
    field: 'industry',
    label: t('opportunity.industry'),
    component: 'Input',
    componentProps: { placeholder: t('common.inputPlaceholder'), maxlength: 50 }
  },
  {
    field: 'keywords',
    label: t('opportunity.keywords'),
    component: 'Select',
    componentProps: { placeholder: t('opportunity.keywordsPlaceholder'), mode: 'tags', style: 'width:100%' }
  },
  {
    field: 'summary',
    label: t('opportunity.summary'),
    component: 'Textarea',
    componentProps: { placeholder: t('opportunity.summaryPlaceholder'), maxlength: 200, showCount: true, rows: 2 }
  },
  {
    field: 'content',
    label: t('opportunity.content'),
    component: 'Textarea',
    componentProps: { placeholder: t('opportunity.contentPlaceholder'), rows: 8 }
  }
])

async function loadForPopulate(id: string, keepId: boolean) {
  const d = await getOpportunityDetail(id)
  formModel.title = keepId ? d.title : `${d.title}（副本）`
  formModel.type = d.type
  formModel.summary = d.summary
  formModel.content = d.content
  formModel.categoryIds = [...d.categoryNames]
}

async function handleSave(status: string) {
  await qFormRef.value?.validate()
  if (isEdit.value) {
    await updateOpportunity({ id: editId as string, ...formModel })
  } else {
    await createOpportunity({ ...formModel })
  }
  message.success(status === 'draft' ? t('common.success') : t('opportunity.publishSuccess'))
  router.push({ path: '/opportunity' })
}

onMounted(async () => {
  await nextTick()
  if (editId) await loadForPopulate(editId, true)
  else if (copyId) await loadForPopulate(copyId, false)
})
</script>
