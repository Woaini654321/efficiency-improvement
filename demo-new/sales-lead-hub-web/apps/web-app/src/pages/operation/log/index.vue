<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col overflow-auto">
    <!-- Z0 统计卡 -->
    <div class="stat-row">
      <StatCard :label="t('log.stat.todayTotal')" :value="todayStats.total" accent="#1677ff">
        <template #icon><ThunderboltOutlined /></template>
      </StatCard>
      <StatCard :label="t('log.stat.sensitive')" :value="todayStats.sensitive"
        :accent="todayStats.sensitive > 0 ? '#ff4d4f' : '#52c41a'">
        <template #icon><WarningOutlined /></template>
      </StatCard>
      <StatCard :label="t('log.stat.failed')" :value="todayStats.failed"
        :accent="todayStats.failed > 0 ? '#faad14' : '#52c41a'">
        <template #icon><CloseCircleOutlined /></template>
      </StatCard>
      <StatCard :label="t('log.stat.operators')" :value="todayStats.operators" accent="#2f54eb">
        <template #icon><TeamOutlined /></template>
      </StatCard>
    </div>

    <!-- Z1 筛选栏 -->
    <div class="filter-bar">
      <span class="filter-title">{{ t('operation.log.DEFAULT') }}</span>
      <a-input-search v-model:value="keyword" :placeholder="t('log.searchPlaceholder')" allow-clear class="w-[220px]" />
      <a-select :value="actionType" class="w-[160px]" allow-clear :placeholder="t('log.actionType')"
        @update:value="(v: any) => (actionType = v ?? '')">
        <a-select-option v-for="o in actionTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
      </a-select>
      <a-select :value="result" class="w-[120px]" allow-clear :placeholder="t('log.result')"
        @update:value="(v: any) => (result = v ?? '')">
        <a-select-option v-for="o in resultOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
      </a-select>
      <a-range-picker :value="(dateRange as any)" value-format="YYYY-MM-DD" class="w-[240px]"
        @update:value="(v: any) => (dateRange = v || [])" />
      <a-button size="small" :type="sensitiveOnly ? 'primary' : 'default'" :danger="sensitiveOnly"
        @click="sensitiveOnly = !sensitiveOnly">
        <template #icon><WarningOutlined /></template>
        {{ t('log.sensitiveOnly') }}
      </a-button>
      <a-button @click="handleExport" :loading="exportLoading">
        <template #icon><ExportOutlined /></template>
        {{ t('common.export') }}
      </a-button>
    </div>

    <!-- Z2 列表 -->
    <div class="flex-1 min-h-0">
      <QBigTable
        :data="filtered"
        :columns="columns"
        :pagination-config="{ pageSize: 50 }"
        height="100%"
      />
    </div>
    <div class="flex items-center justify-between text-[12px] text-[hsl(var(--secondary-text))] mt-2">
      <span><SafetyOutlined /> {{ t('log.retentionTip') }}</span>
      <span>{{ t('common.totalItems', { n: filtered.length }) }}</span>
    </div>

    <!-- 详情 Drawer -->
    <a-drawer v-model:open="drawerOpen" :title="t('log.detailTitle')" :width="640" placement="right">
      <a-descriptions v-if="current" bordered size="small" :column="1">
        <a-descriptions-item :label="t('common.createdAt')">{{ current.createdAt || '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('log.operator')">{{ current.operatorName || '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('log.actionType')">
          <a-tag :color="sensitive(current.actionType) ? 'red' : 'blue'">{{ t('dict.actionType.' + current.actionType) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('log.target')">{{ current.target || '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('common.status')">
          <a-tag :color="current.result === 'success' ? 'green' : 'red'">{{ t('dict.result.' + current.result) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('log.ip')">{{ current.ipAddress || '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('log.userAgent')">{{ current.userAgent || '--' }}</a-descriptions-item>
      </a-descriptions>

      <div v-if="current" class="mt-4 grid grid-cols-2 gap-3">
        <div>
          <h4 class="text-[14px] font-semibold mb-2 diff-title-before">{{ t('log.beforeSnapshot') }}</h4>
          <pre class="snapshot snapshot-before">{{ snapshotText(current.beforeSnapshot) }}</pre>
        </div>
        <div>
          <h4 class="text-[14px] font-semibold mb-2 diff-title-after">{{ t('log.afterSnapshot') }}</h4>
          <pre class="snapshot snapshot-after">{{ snapshotText(current.afterSnapshot) }}</pre>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined, WarningOutlined, CloseCircleOutlined, TeamOutlined,
  ExportOutlined, EyeOutlined, SafetyOutlined,
  SendOutlined, DownCircleOutlined, DeleteOutlined, SafetyCertificateOutlined,
  LockOutlined, FolderOutlined, LoginOutlined, RiseOutlined
} from '@ant-design/icons-vue'
import StatCard from '@/components/stat-card/index.vue'
import { QBigTable } from '@/components/q-big-table'
import type { TableColumn } from '@/components/q-big-table'
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

// 敏感操作：删除 / 角色变更 / 隔离配置变更
const SENSITIVE = ['delete', 'role_change', 'isolation_change']
const sensitive = (type: string) => SENSITIVE.includes(type)

const fallbackAction = { icon: SendOutlined, cls: 'act-default' }
const actionMeta: Record<string, { icon: any; cls: string }> = {
  publish: { icon: SendOutlined, cls: 'act-publish' },
  archive: { icon: DownCircleOutlined, cls: 'act-archive' },
  delete: { icon: DeleteOutlined, cls: 'act-delete' },
  role_change: { icon: SafetyCertificateOutlined, cls: 'act-role' },
  isolation_change: { icon: LockOutlined, cls: 'act-isolation' },
  category_change: { icon: FolderOutlined, cls: 'act-category' },
  login: { icon: LoginOutlined, cls: 'act-login' },
  sla_escalation: { icon: RiseOutlined, cls: 'act-sla' }
}
function avatarIdx(name: string): number {
  return (name || '?').charCodeAt(0) % 8
}

const allItems = ref<AuditLogItem[]>([])
const keyword = ref('')
const actionType = ref('')
const result = ref('')
const sensitiveOnly = ref(false)
const dstr = (n = 0) => {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toISOString().slice(0, 10)
}
const dateRange = ref<string[]>([dstr(7), dstr(0)])
const exportLoading = ref(false)

const actionTypeOptions = computed(() =>
  ['publish', 'archive', 'delete', 'role_change', 'isolation_change', 'category_change', 'login', 'sla_escalation'].map((v) => ({
    label: t('dict.actionType.' + v),
    value: v
  }))
)
const resultOptions = computed(() => [
  { label: t('dict.result.success'), value: 'success' },
  { label: t('dict.result.failure'), value: 'failure' }
])

function renderOperator(row: AuditLogItem) {
  return (
    <div class="flex items-center gap-2">
      <span class={'avatar av-' + avatarIdx(row.operatorName)}>{(row.operatorName || '?').charAt(0)}</span>
      <span class="text-[13px]">{row.operatorName}</span>
    </div>
  )
}
function renderActionType(row: AuditLogItem) {
  const meta = actionMeta[row.actionType] || fallbackAction
  const Icon = meta.icon
  return (
    <div class="flex items-center gap-2">
      <span class={'act-badge ' + meta.cls}><Icon /></span>
      <a-tag color={sensitive(row.actionType) ? 'red' : 'blue'}>{t('dict.actionType.' + row.actionType)}</a-tag>
    </div>
  )
}
function renderRowAction(row: AuditLogItem) {
  return (
    <a-button type="link" size="small" onClick={() => openDetail(row)}>
      <EyeOutlined /> {t('common.detail')}
    </a-button>
  )
}

const columns = computed<TableColumn[]>(() => [
  { field: 'createdAt', title: t('common.createdAt'), width: 170 },
  { field: 'operator', title: t('log.operator'), width: 160, slots: { default: ({ row }: any) => renderOperator(row) } },
  { field: 'actionType', title: t('log.actionType'), width: 170, slots: { default: ({ row }: any) => renderActionType(row) } },
  { field: 'target', title: t('log.target'), minWidth: 240 },
  {
    field: 'result',
    title: t('log.result'),
    width: 90,
    slots: { default: ({ row }: any) => <a-tag color={row.result === 'success' ? 'green' : 'red'}>{t('dict.result.' + row.result)}</a-tag> }
  },
  { field: 'ipAddress', title: t('log.ip'), width: 140 },
  { field: 'action', title: t('common.action'), width: 90, fixed: 'right', slots: { default: ({ row }: any) => renderRowAction(row) } }
])

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const [start, end] = dateRange.value || []
  return allItems.value.filter((i) => {
    if (kw && !(i.operatorName + i.target).toLowerCase().includes(kw)) return false
    if (actionType.value && i.actionType !== actionType.value) return false
    if (result.value && i.result !== result.value) return false
    if (sensitiveOnly.value && !sensitive(i.actionType)) return false
    if (start && end) {
      const d = (i.createdAt || '').slice(0, 10)
      if (d < start || d > end) return false
    }
    return true
  })
})

const todayStats = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  const list = allItems.value.filter((i) => (i.createdAt || '').slice(0, 10) === today)
  const ops = new Set(list.map((i) => i.operatorName))
  return {
    total: list.length,
    sensitive: list.filter((i) => sensitive(i.actionType)).length,
    failed: list.filter((i) => i.result === 'failure').length,
    operators: ops.size
  }
})

function handleExport() {
  if (exportLoading.value) return
  exportLoading.value = true
  window.setTimeout(() => {
    if (filtered.value.length > 10000) {
      message.warning(t('log.exportLimit'))
    } else {
      message.success(t('log.exportSuccess'))
    }
    exportLoading.value = false
  }, 800)
}

const drawerOpen = ref(false)
const current = ref<AuditLogItem | null>(null)
function snapshotText(snap: Record<string, unknown> | null) {
  return snap ? JSON.stringify(snap, null, 2) : '--'
}
function openDetail(row: AuditLogItem) {
  current.value = row
  drawerOpen.value = true
}

async function load() {
  const res = await getAuditLogList({ pageNumber: 1, pageSize: 999 })
  allItems.value = res.records
}

onMounted(load)
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.filter-title {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--text));
  margin-right: 4px;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.av-0 { background: #1890ff; }
.av-1 { background: #52c41a; }
.av-2 { background: #fa8c16; }
.av-3 { background: #722ed1; }
.av-4 { background: #eb2f96; }
.av-5 { background: #13c2c2; }
.av-6 { background: #2f54eb; }
.av-7 { background: #faad14; }
.act-badge {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}
.act-default { background: #f5f5f5; color: #666; }
.act-publish { background: #e6f7ff; color: #1677ff; }
.act-archive { background: #fff7e6; color: #fa8c16; }
.act-delete { background: #fff1f0; color: #ff4d4f; }
.act-role { background: #f9f0ff; color: #722ed1; }
.act-isolation { background: #fff0f6; color: #eb2f96; }
.act-category { background: #e6fffb; color: #13c2c2; }
.act-login { background: #f6ffed; color: #52c41a; }
.act-sla { background: #fffbe6; color: #faad14; }
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
.diff-title-before { color: hsl(var(--success, 142 71% 45%)); }
.diff-title-after { color: hsl(var(--error)); }
.snapshot-before {
  background: hsl(var(--success, 142 71% 45%) / 0.08);
  border: 1px solid hsl(var(--success, 142 71% 45%) / 0.3);
}
.snapshot-after {
  background: hsl(var(--error) / 0.08);
  border: 1px solid hsl(var(--error) / 0.3);
}
</style>
