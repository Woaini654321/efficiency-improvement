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
import { getRequirementList } from '@/apis/requirement/requirementApi'

defineOptions({ name: 'RequirementList' })
definePage({
  name: 'RequirementList',
  meta: {
    layout: false,
    menu: true,
    title: 'requirement.list'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const tableRef = ref<QBigTableExpose | null>(null)

const urgencyColor: Record<string, string> = {
  critical: 'red',
  urgent: 'orange',
  normal: 'default'
}
const statusColor: Record<string, string> = {
  Pending: 'orange',
  Collecting: 'blue',
  Adopted: 'green',
  Closed: 'default'
}

const urgencyOptions = computed(() => [
  { label: t('dict.urgency.critical'), value: 'critical' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.normal'), value: 'normal' }
])
const statusOptions = computed(() => [
  { label: t('dict.reqStatus.Pending'), value: 'Pending' },
  { label: t('dict.reqStatus.Collecting'), value: 'Collecting' },
  { label: t('dict.reqStatus.Adopted'), value: 'Adopted' },
  { label: t('dict.reqStatus.Closed'), value: 'Closed' }
])

const searchConfig: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('requirement.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'urgency',
    label: t('requirement.urgency'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: urgencyOptions.value }
  },
  {
    field: 'status',
    label: t('common.status'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:150px', options: statusOptions.value }
  }
]

const toolbarConfig: ToolbarButton[] = [
  { label: t('requirement.add'), type: 'primary', onClick: () => { router.push({ path: '/requirement/form' }) } }
]

const columns: TableColumn[] = [
  {
    field: 'title',
    title: t('requirement.title'),
    minWidth: 280,
    slots: {
      default: ({ row }: any) => (
        <span class="flex items-center gap-1">
          {row.isPinned ? <a-tag color="red">{t('requirement.pinned')}</a-tag> : null}
          <a class="text-[hsl(var(--primary))]" onClick={() => goDetail(row.id)}>{row.title}</a>
        </span>
      )
    }
  },
  {
    field: 'urgency',
    title: t('requirement.urgency'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={urgencyColor[row.urgency] ?? 'default'}>{t('dict.urgency.' + row.urgency)}</a-tag> }
  },
  {
    field: 'status',
    title: t('common.status'),
    width: 120,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] ?? 'default'}>{t('dict.reqStatus.' + row.status)}</a-tag> }
  },
  { field: 'industry', title: t('requirement.industry'), width: 130, slots: { default: ({ row }: any) => <span>{row.industry ?? '--'}</span> } },
  { field: 'publisherName', title: t('requirement.publisher'), width: 110 },
  { field: 'publisherDeptName', title: t('requirement.dept'), width: 150 },
  { field: 'responseCount', title: t('requirement.responseCount'), width: 100 },
  { field: 'createdAt', title: t('common.createdAt'), width: 160, slots: { default: ({ row }: any) => <span>{row.createdAt ?? '--'}</span> } },
  {
    title: t('common.action'),
    width: 130,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => (
        <span class="flex gap-1">
          <a-button type="link" onClick={() => goDetail(row.id)}>{t('common.view')}</a-button>
          <a-button type="link" onClick={() => router.push({ path: '/requirement/form', query: { id: row.id } })}>{t('common.edit')}</a-button>
        </span>
      )
    }
  }
]

function goDetail(id: string) {
  router.push({ path: '/requirement/detail', query: { id } })
}

async function queryApi({ page }: any, searchParams: any) {
  const result = await getRequirementList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}
</script>
