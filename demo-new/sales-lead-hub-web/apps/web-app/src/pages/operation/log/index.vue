<template>
  <div class="h-full p-[16px] bg-white rounded">
    <QBigTable
      ref="tableRef"
      :search-config="searchConfig"
      :columns="columns"
      :query-api="queryApi"
      height="100%"
    />

    <a-drawer
      v-model:open="drawerOpen"
      :title="t('log.detailTitle')"
      :width="640"
      placement="right"
    >
      <a-descriptions v-if="current" bordered size="small" :column="1">
        <a-descriptions-item :label="t('common.createdAt')">{{ current.createdAt ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('log.operator')">{{ current.operatorName ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('log.actionType')">
          <a-tag :color="sensitive(current.actionType) ? 'red' : 'default'">{{ t('dict.actionType.' + current.actionType) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('log.target')">{{ current.target ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('common.status')">
          <a-tag :color="current.result === 'success' ? 'green' : 'red'">{{ t('dict.result.' + current.result) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('log.ip')">{{ current.ipAddress ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('log.userAgent')">{{ current.userAgent ?? '--' }}</a-descriptions-item>
      </a-descriptions>

      <div v-if="current" class="mt-4 grid grid-cols-2 gap-3">
        <div>
          <h4 class="text-[14px] font-semibold mb-2">{{ t('log.beforeSnapshot') }}</h4>
          <pre class="snapshot snapshot-before">{{ snapshotText(current.beforeSnapshot) }}</pre>
        </div>
        <div>
          <h4 class="text-[14px] font-semibold mb-2">{{ t('log.afterSnapshot') }}</h4>
          <pre class="snapshot snapshot-after">{{ snapshotText(current.afterSnapshot) }}</pre>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { QBigTable } from '@/components/q-big-table'
import type { QBigTableExpose, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { getAuditLogList } from '@/apis/auditLog/auditLogApi'
import type { AuditLogItem } from '@/apis/auditLog/types'

defineOptions({ name: 'OperationLog' })
definePage({
  name: 'OperationLog',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.log.DEFAULT'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const tableRef = ref<QBigTableExpose | null>(null)
const drawerOpen = ref(false)
const current = ref<AuditLogItem | null>(null)

// 敏感操作：删除 / 角色变更 / 隔离配置变更
const SENSITIVE = ['delete', 'role_change', 'isolation_change']
const sensitive = (type: string) => SENSITIVE.includes(type)

const actionTypeOptions = computed(() =>
  ['publish', 'archive', 'delete', 'role_change', 'isolation_change', 'category_change', 'login', 'sla_escalation'].map(v => ({
    label: t('dict.actionType.' + v),
    value: v
  }))
)
const resultOptions = computed(() => [
  { label: t('dict.result.success'), value: 'success' },
  { label: t('dict.result.failure'), value: 'failure' }
])

const searchConfig: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('log.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'actionType',
    label: t('log.actionType'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:160px', options: actionTypeOptions.value }
  },
  {
    field: 'result',
    label: t('log.result'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:120px', options: resultOptions.value }
  }
]

const columns: TableColumn[] = [
  { field: 'createdAt', title: t('common.createdAt'), width: 170 },
  { field: 'operatorName', title: t('log.operator'), width: 120 },
  {
    field: 'actionType',
    title: t('log.actionType'),
    width: 140,
    slots: { default: ({ row }: any) => <a-tag color={sensitive(row.actionType) ? 'red' : 'default'}>{t('dict.actionType.' + row.actionType)}</a-tag> }
  },
  { field: 'target', title: t('log.target'), minWidth: 260 },
  {
    field: 'result',
    title: t('log.result'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={row.result === 'success' ? 'green' : 'red'}>{t('dict.result.' + row.result)}</a-tag> }
  },
  { field: 'ipAddress', title: t('log.ip'), width: 140 },
  {
    title: t('common.action'),
    width: 100,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => <a-button type="link" onClick={() => openDetail(row)}>{t('common.detail')}</a-button>
    }
  }
]

function snapshotText(snap: Record<string, unknown> | null) {
  return snap ? JSON.stringify(snap, null, 2) : '--'
}

function openDetail(row: AuditLogItem) {
  current.value = row
  drawerOpen.value = true
}

async function queryApi({ page }: any, searchParams: any) {
  const result = await getAuditLogList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}
</script>

<style scoped>
.snapshot {
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
  max-height: 320px;
}
.snapshot-before {
  background: hsl(var(--success, 142 71% 45%) / 0.08);
  border: 1px solid hsl(var(--success, 142 71% 45%) / 0.3);
}
.snapshot-after {
  background: hsl(var(--error) / 0.08);
  border: 1px solid hsl(var(--error) / 0.3);
}
</style>
