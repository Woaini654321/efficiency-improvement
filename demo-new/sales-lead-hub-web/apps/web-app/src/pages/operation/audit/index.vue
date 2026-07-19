<template>
  <div class="h-full p-[16px] bg-white rounded">
    <QBigTable
      ref="tableRef"
      :search-config="searchConfig"
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
import type { QBigTableExpose, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { getAuditList, changeAuditStatus, changeAuditPin, deleteAudit } from '@/apis/audit/auditApi'

defineOptions({ name: 'OperationAudit' })
definePage({
  name: 'OperationAudit',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.audit.DEFAULT'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const tableRef = ref<QBigTableExpose | null>(null)

const contentTypeColor: Record<string, string> = {
  opportunity: 'blue',
  request: 'orange'
}
const statusColor: Record<string, string> = {
  published: 'green',
  archived: 'red',
  pending: 'orange',
  collecting: 'blue',
  adopted: 'green',
  closed: 'default'
}
// 允许下架的状态（已下架/已关闭不再提供下架）
const canArchive = (status: string) => !['archived', 'closed'].includes(status)

const contentTypeOptions = computed(() => [
  { label: t('dict.auditContentType.opportunity'), value: 'opportunity' },
  { label: t('dict.auditContentType.request'), value: 'request' }
])
const statusOptions = computed(() => [
  { label: t('dict.auditStatus.published'), value: 'published' },
  { label: t('dict.auditStatus.archived'), value: 'archived' },
  { label: t('dict.auditStatus.pending'), value: 'pending' },
  { label: t('dict.auditStatus.collecting'), value: 'collecting' },
  { label: t('dict.auditStatus.adopted'), value: 'adopted' },
  { label: t('dict.auditStatus.closed'), value: 'closed' }
])

const searchConfig: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('audit.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'contentType',
    label: t('audit.contentType'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:150px', options: contentTypeOptions.value }
  },
  {
    field: 'status',
    label: t('common.status'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: statusOptions.value }
  }
]

const columns: TableColumn[] = [
  {
    field: 'title',
    title: t('audit.title'),
    minWidth: 300,
    slots: {
      default: ({ row }: any) => (
        <span class="flex items-center gap-1">
          {row.isPinned ? <a-tag color="red">{t('audit.pinned')}</a-tag> : null}
          <a class="text-[hsl(var(--primary))]" onClick={() => handleView(row)}>{row.title}</a>
        </span>
      )
    }
  },
  {
    field: 'contentType',
    title: t('audit.contentType'),
    width: 120,
    slots: { default: ({ row }: any) => <a-tag color={contentTypeColor[row.contentType] ?? 'default'}>{t('dict.auditContentType.' + row.contentType)}</a-tag> }
  },
  { field: 'publisherName', title: t('audit.publisher'), width: 120 },
  {
    field: 'status',
    title: t('common.status'),
    width: 110,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] ?? 'default'}>{t('dict.auditStatus.' + row.status)}</a-tag> }
  },
  { field: 'publishedAt', title: t('common.publishedAt'), width: 170, slots: { default: ({ row }: any) => <span>{row.publishedAt ?? '--'}</span> } },
  {
    title: t('common.action'),
    width: 280,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => (
        <span class="flex gap-1">
          <a-button type="link" onClick={() => handleView(row)}>{t('common.view')}</a-button>
          <a-button type="link" onClick={() => handleEdit(row)}>{t('common.edit')}</a-button>
          <a-button type="link" onClick={() => handlePin(row)}>{row.isPinned ? t('audit.unpin') : t('audit.pin')}</a-button>
          {canArchive(row.status) ? (
            <a-button type="link" danger onClick={() => handleArchive(row)}>{t('audit.archive')}</a-button>
          ) : null}
          <a-popconfirm title={t('audit.deleteConfirm')} placement="topRight" onConfirm={() => handleDelete(row)}>
            <a-button type="link" danger>{t('common.delete')}</a-button>
          </a-popconfirm>
        </span>
      )
    }
  }
]

function targetPath(row: any, sub: 'detail' | 'form') {
  return row.contentType === 'request' ? `/requirement/${sub}` : `/opportunity/${sub}`
}
function handleView(row: any) {
  router.push({ path: targetPath(row, 'detail'), query: { id: row.id } })
}
function handleEdit(row: any) {
  router.push({ path: targetPath(row, 'form'), query: { id: row.id } })
}

async function queryApi({ page }: any, searchParams: any) {
  const result = await getAuditList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}

async function handlePin(row: any) {
  await changeAuditPin({ id: row.id, isPinned: !row.isPinned })
  message.success(t('common.success'))
  tableRef.value?.refresh()
}

function handleArchive(row: any) {
  Modal.confirm({
    title: t('audit.archiveConfirm'),
    onOk: async () => {
      await changeAuditStatus({ id: row.id, status: 'archived' })
      message.success(t('common.success'))
      tableRef.value?.refresh()
    }
  })
}

async function handleDelete(row: any) {
  await deleteAudit(row.id)
  message.success(t('common.success'))
  tableRef.value?.refresh()
}
</script>
