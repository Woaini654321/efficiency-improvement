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
import { message, Modal } from 'ant-design-vue'
import { QBigTable } from '@/components/q-big-table'
import type { QBigTableExpose, ToolbarButton, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { getOpportunityList, changeOpportunityStatus } from '@/apis/opportunity/opportunityApi'

defineOptions({ name: 'OpportunityList' })
definePage({
  name: 'OpportunityList',
  meta: {
    layout: false,
    menu: true,
    title: 'opportunity.list'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const tableRef = ref<QBigTableExpose | null>(null)

const typeColor: Record<string, string> = {
  product_info: 'blue',
  solution: 'green',
  success_case: 'orange'
}
const statusColor: Record<string, string> = {
  published: 'green',
  archived: 'red',
  draft: 'default'
}

const typeOptions = computed(() => [
  { label: t('dict.oppType.product_info'), value: 'product_info' },
  { label: t('dict.oppType.solution'), value: 'solution' },
  { label: t('dict.oppType.success_case'), value: 'success_case' }
])
const statusOptions = computed(() => [
  { label: t('dict.oppStatus.published'), value: 'published' },
  { label: t('dict.oppStatus.archived'), value: 'archived' },
  { label: t('dict.oppStatus.draft'), value: 'draft' }
])

const searchConfig: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('opportunity.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'type',
    label: t('opportunity.type'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:150px', options: typeOptions.value }
  },
  {
    field: 'status',
    label: t('common.status'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: statusOptions.value }
  }
]

const toolbarConfig: ToolbarButton[] = [
  { label: t('opportunity.add'), type: 'primary', onClick: () => { router.push({ path: '/opportunity/form' }) } }
]

const columns: TableColumn[] = [
  {
    field: 'title',
    title: t('opportunity.title'),
    minWidth: 260,
    slots: {
      default: ({ row }: any) => (
        <span class="flex items-center gap-1">
          {row.isPinned ? <a-tag color="red">{t('opportunity.pinned')}</a-tag> : null}
          <a class="text-[hsl(var(--primary))]" onClick={() => goDetail(row.id)}>{row.title}</a>
        </span>
      )
    }
  },
  {
    field: 'type',
    title: t('opportunity.type'),
    width: 120,
    slots: { default: ({ row }: any) => <a-tag color={typeColor[row.type] ?? 'default'}>{t('dict.oppType.' + row.type)}</a-tag> }
  },
  {
    field: 'categoryNames',
    title: t('opportunity.category'),
    minWidth: 180,
    slots: { default: ({ row }: any) => (row.categoryNames ?? []).map((c: string) => <a-tag>{c}</a-tag>) }
  },
  { field: 'publisherName', title: t('opportunity.publisher'), width: 110 },
  { field: 'publisherDeptName', title: t('opportunity.dept'), width: 150 },
  { field: 'viewCount', title: t('opportunity.views'), width: 90 },
  {
    field: 'status',
    title: t('common.status'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] ?? 'default'}>{t('dict.oppStatus.' + row.status)}</a-tag> }
  },
  { field: 'publishedAt', title: t('common.publishedAt'), width: 160, slots: { default: ({ row }: any) => <span>{row.publishedAt ?? '--'}</span> } },
  {
    title: t('common.action'),
    width: 160,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => (
        <span class="flex gap-1">
          <a-button type="link" onClick={() => goDetail(row.id)}>{t('common.view')}</a-button>
          <a-button type="link" onClick={() => router.push({ path: '/opportunity/form', query: { id: row.id } })}>{t('common.edit')}</a-button>
          {row.status === 'published' ? (
            <a-button type="link" danger onClick={() => handleArchive(row.id)}>{t('opportunity.archive')}</a-button>
          ) : null}
        </span>
      )
    }
  }
]

function goDetail(id: string) {
  router.push({ path: '/opportunity/detail', query: { id } })
}

async function queryApi({ page }: any, searchParams: any) {
  const result = await getOpportunityList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}

function handleArchive(id: string) {
  Modal.confirm({
    title: t('opportunity.archiveConfirm'),
    onOk: async () => {
      await changeOpportunityStatus(id, 'archived')
      message.success(t('common.success'))
      tableRef.value?.refresh()
    }
  })
}
</script>
