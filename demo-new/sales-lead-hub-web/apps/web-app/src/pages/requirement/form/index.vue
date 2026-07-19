<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto flex flex-col">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-bold">{{ isEdit ? t('requirement.editTitle') : t('requirement.add') }}</h2>
      <a-button @click="router.back()">{{ t('common.back') }}</a-button>
    </div>

    <a-alert v-if="isCritical" type="error" show-icon class="mb-3" :message="t('requirement.criticalTip')" />

    <div class="flex-1">
      <QForm ref="qFormRef" :schemas="schemas" v-model:model="formModel" />
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-[hsl(var(--line))] mt-4">
      <a-button @click="handleSave('draft')">{{ t('common.save') }}</a-button>
      <a-button type="primary" @click="handleSave('published')">{{ t('requirement.publish') }}</a-button>
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
import { getRequirementDetail, createRequirement, updateRequirement } from '@/apis/requirement/requirementApi'

defineOptions({ name: 'RequirementForm' })
definePage({
  name: 'RequirementForm',
  meta: {
    layout: false,
    menu: false,
    title: 'requirement.form'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const qFormRef = ref()

const editId = route.query.id as string | undefined
const isEdit = computed(() => !!editId)

const formModel = reactive<{
  title: string
  urgency: string
  industry: string
  keywords: string[]
  categoryIds: string[]
  visibilityType: string
  description: string
}>({
  title: '',
  urgency: 'normal',
  industry: '',
  keywords: [],
  categoryIds: [],
  visibilityType: 'all',
  description: ''
})

const isCritical = computed(() => formModel.urgency === 'critical')

const urgencyOptions = computed(() => [
  { label: t('dict.urgency.normal'), value: 'normal' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.critical'), value: 'critical' }
])
const visibilityOptions = computed(() => [
  { label: t('requirement.visibilityType.all'), value: 'all' },
  { label: t('requirement.visibilityType.dept'), value: 'dept' },
  { label: t('requirement.visibilityType.personnel'), value: 'personnel' }
])

const schemas = computed(() => [
  {
    field: 'title',
    label: t('requirement.title'),
    component: 'Input',
    rules: [{ required: true, message: t('requirement.titlePlaceholder') }],
    componentProps: { placeholder: t('requirement.titlePlaceholder'), maxlength: 100, showCount: true }
  },
  {
    field: 'urgency',
    label: t('requirement.urgency'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), options: urgencyOptions.value, style: 'width:100%' }
  },
  {
    field: 'industry',
    label: t('requirement.industry'),
    component: 'Input',
    componentProps: { placeholder: t('common.inputPlaceholder'), maxlength: 50 }
  },
  {
    field: 'keywords',
    label: t('requirement.keywords'),
    component: 'Select',
    componentProps: { placeholder: t('requirement.keywordsPlaceholder'), mode: 'tags', style: 'width:100%' }
  },
  {
    field: 'categoryIds',
    label: t('requirement.category'),
    component: 'Select',
    componentProps: { placeholder: t('requirement.categoryPlaceholder'), mode: 'tags', style: 'width:100%' }
  },
  {
    field: 'visibilityType',
    label: t('requirement.visibility'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), options: visibilityOptions.value, style: 'width:100%' }
  },
  {
    field: 'description',
    label: t('requirement.description'),
    component: 'Textarea',
    rules: [{ required: true, message: t('requirement.descriptionPlaceholder') }],
    componentProps: { placeholder: t('requirement.descriptionPlaceholder'), rows: 8 }
  }
])

async function loadForPopulate(id: string) {
  const d = await getRequirementDetail(id)
  formModel.title = d.title
  formModel.urgency = d.urgency
  formModel.industry = d.industry
  formModel.visibilityType = d.visibilityType
  formModel.description = d.description
  formModel.categoryIds = [...d.categoryNames]
}

async function handleSave(status: string) {
  await qFormRef.value?.validate()
  if (isEdit.value) {
    await updateRequirement({ id: editId as string, ...formModel })
  } else {
    await createRequirement({ ...formModel })
  }
  message.success(status === 'draft' ? t('common.success') : t('requirement.publishSuccess'))
  router.push({ path: '/requirement' })
}

onMounted(async () => {
  await nextTick()
  if (editId) await loadForPopulate(editId)
})
</script>
