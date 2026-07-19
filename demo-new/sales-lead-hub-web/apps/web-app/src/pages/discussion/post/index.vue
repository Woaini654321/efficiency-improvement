<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto flex flex-col">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-bold">{{ t('discussion.post') }}</h2>
      <a-button @click="router.back()">{{ t('common.back') }}</a-button>
    </div>

    <div class="flex-1">
      <QForm ref="qFormRef" :schemas="schemas" v-model:model="formModel" />
    </div>

    <div class="flex justify-end gap-2 pt-4 border-t border-[hsl(var(--line))] mt-4">
      <a-button @click="router.back()">{{ t('common.cancel') }}</a-button>
      <a-button type="primary" @click="handleSubmit">{{ t('discussion.publishPost') }}</a-button>
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
import { createDiscussion } from '@/apis/discussion/discussionApi'

defineOptions({ name: 'DiscussionPost' })
definePage({
  name: 'DiscussionPost',
  meta: {
    layout: false,
    menu: false,
    title: 'discussion.post'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const qFormRef = ref()

const formModel = reactive<{
  title: string
  topic: string
  content: string
}>({
  title: '',
  topic: '',
  content: ''
})

const topicOptions = computed(() => [
  { label: t('discussion.topic.business'), value: 'business' },
  { label: t('discussion.topic.solution'), value: 'solution' },
  { label: t('discussion.topic.experience'), value: 'experience' },
  { label: t('discussion.topic.industry'), value: 'industry' },
  { label: t('discussion.topic.complaint'), value: 'complaint' }
])

const schemas = computed(() => [
  {
    field: 'title',
    label: t('discussion.title'),
    component: 'Input',
    rules: [{ required: true, message: t('discussion.titlePlaceholder') }],
    componentProps: { placeholder: t('discussion.titlePlaceholder'), maxlength: 100, showCount: true }
  },
  {
    field: 'topic',
    label: t('discussion.topicLabel'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), options: topicOptions.value, style: 'width:100%' }
  },
  {
    field: 'content',
    label: t('discussion.content'),
    component: 'Textarea',
    rules: [{ required: true, message: t('discussion.contentPlaceholder') }],
    componentProps: { placeholder: t('discussion.contentPlaceholder'), rows: 10, maxlength: 5000, showCount: true }
  }
])

async function handleSubmit() {
  await qFormRef.value?.validate()
  await createDiscussion({ ...formModel })
  message.success(t('discussion.publishSuccess'))
  router.push({ path: '/discussion' })
}
</script>
