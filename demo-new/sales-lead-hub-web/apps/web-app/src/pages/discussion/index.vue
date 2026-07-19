<template>
  <div class="h-full p-[16px] bg-white rounded">
    <QBigTable
      ref="tableRef"
      :search-config="searchConfig"
      :toolbar-config="toolbarConfig"
      :columns="columns"
      :query-api="queryApi"
      height="100%"
    />
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { QBigTable } from '@/components/q-big-table'
import type { QBigTableExpose, ToolbarButton, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { getDiscussionList } from '@/apis/discussion/discussionApi'

defineOptions({ name: 'DiscussionList' })
definePage({
  name: 'DiscussionList',
  meta: {
    layout: false,
    menu: true,
    title: 'discussion.list'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const tableRef = ref<QBigTableExpose | null>(null)

const topicColor: Record<string, string> = {
  business: 'blue',
  solution: 'orange',
  experience: 'green',
  industry: 'default',
  complaint: 'red'
}

const topicOptions = computed(() => [
  { label: t('discussion.topic.business'), value: 'business' },
  { label: t('discussion.topic.solution'), value: 'solution' },
  { label: t('discussion.topic.experience'), value: 'experience' },
  { label: t('discussion.topic.industry'), value: 'industry' },
  { label: t('discussion.topic.complaint'), value: 'complaint' }
])

const searchConfig: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('discussion.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'topic',
    label: t('discussion.topicLabel'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:160px', options: topicOptions.value }
  }
]

const toolbarConfig: ToolbarButton[] = [
  { label: t('discussion.post'), type: 'primary', onClick: () => { router.push({ path: '/discussion/post' }) } }
]

const columns: TableColumn[] = [
  {
    field: 'title',
    title: t('discussion.title'),
    minWidth: 320,
    slots: {
      default: ({ row }: any) => (
        <span class="flex items-center gap-1">
          {row.isHot ? <a-tag color="red">{t('discussion.hot')}</a-tag> : null}
          <a class="text-[hsl(var(--primary))]" onClick={() => goDetail(row.id)}>{row.title}</a>
        </span>
      )
    }
  },
  {
    field: 'topic',
    title: t('discussion.topicLabel'),
    width: 120,
    slots: { default: ({ row }: any) => <a-tag color={topicColor[row.topic] ?? 'default'}>{t('discussion.topic.' + row.topic)}</a-tag> }
  },
  { field: 'authorName', title: t('discussion.author'), width: 120 },
  { field: 'replyCount', title: t('discussion.replies'), width: 90 },
  { field: 'viewCount', title: t('discussion.views'), width: 90 },
  { field: 'createdAt', title: t('common.createdAt'), width: 170 },
  {
    title: t('common.action'),
    width: 100,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => <a-button type="link" onClick={() => goDetail(row.id)}>{t('common.view')}</a-button>
    }
  }
]

function goDetail(id: string) {
  router.push({ path: '/discussion/detail', query: { id } })
}

async function queryApi({ page }: any, searchParams: any) {
  const result = await getDiscussionList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}
</script>
