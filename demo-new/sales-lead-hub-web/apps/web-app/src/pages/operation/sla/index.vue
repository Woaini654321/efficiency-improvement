<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <h2 class="text-[18px] font-bold mb-4">{{ t('sla.pageTitle') }}</h2>

    <!-- Z1 筛选栏 -->
    <div class="filter-bar">
      <a-select :value="urgencyFilter" :placeholder="t('sla.urgency')" allow-clear class="w-[140px]"
        :options="urgencyOptions" @update:value="(v: any) => (urgencyFilter = v)" />
      <a-select :value="slaStatusFilter" :placeholder="t('common.status')" allow-clear class="w-[140px]"
        :options="slaStatusOptions" @update:value="(v: any) => onStatusSelect(v)" />
      <a-range-picker :value="(dateRange as any)" value-format="YYYY-MM-DD" class="w-[240px]" allow-clear
        @update:value="(v: any) => (dateRange = v || [])" />
    </div>

    <!-- Z2 概览统计卡 -->
    <a-row :gutter="16" class="mb-3">
      <a-col :xs="12" :sm="6">
        <div class="stat-box"><a-statistic :title="t('sla.stat.total')" :value="stats.totalRequests" /></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-box"><a-statistic :title="t('sla.stat.timelyRate')" :value="stats.timelyRate" suffix="%" /></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-box"><a-statistic :title="t('sla.stat.responded')" :value="stats.respondedCount" /></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-box"><a-statistic :title="t('sla.stat.maxOverdue')" :value="stats.maxOverdueText || '--'" /></div>
      </a-col>
    </a-row>

    <!-- Z3 可点击汇总卡（点击筛选表格） -->
    <a-row :gutter="16" class="mb-3">
      <a-col v-for="c in summaryCards" :key="c.key" :xs="12" :sm="6">
        <StatCard :label="c.label" :value="c.value" :accent="c.color" clickable
          :active="slaStatusFilter === c.key" @click="toggleCard(c.key)">
          <template #icon><component :is="c.icon" /></template>
        </StatCard>
      </a-col>
    </a-row>

    <!-- 表格 -->
    <QBigTable
      ref="tableRef"
      :data="filtered"
      :columns="columns"
      :selectable="true"
      :pagination-config="{ pageSize: 8 }"
      height="440"
      @checkbox-change="onCheckboxChange"
    />

    <!-- 批量催办栏 -->
    <div v-if="selectedRows.length" class="batch-bar">
      <span>{{ t('sla.selectedN', { n: selectedRows.length }) }}</span>
      <a-button type="primary" size="small" @click="openUrge(selectedIds)">
        <template #icon><BellOutlined /></template>{{ t('sla.batchUrge') }}
      </a-button>
      <a-button size="small" @click="clearSelection">{{ t('sla.clearSelect') }}</a-button>
    </div>

    <!-- 详情 Drawer -->
    <a-drawer v-model:open="drawerOpen" :title="t('sla.detailTitle')" :width="560">
      <template v-if="current">
        <a-descriptions bordered size="small" :column="1">
          <a-descriptions-item :label="t('sla.title')">{{ current.title }}</a-descriptions-item>
          <a-descriptions-item :label="t('sla.publisher')">{{ current.publisherName || '--' }}</a-descriptions-item>
          <a-descriptions-item :label="t('sla.urgency')">
            <a-tag :color="urgencyColor[current.urgency] ?? 'default'">{{ t('dict.urgency.' + current.urgency) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="t('common.status')">
            <a-tag :color="slaColor[current.slaStatus] ?? 'default'">{{ t('dict.slaStatus.' + current.slaStatus) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="t('sla.createdAt')">{{ current.createdAt || '--' }}</a-descriptions-item>
          <a-descriptions-item :label="t('sla.deadline')">{{ current.deadline || '--' }}</a-descriptions-item>
          <a-descriptions-item :label="t('sla.remaining')">
            <span :class="renderRemaining(current).cls">{{ renderRemaining(current).text }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="t('sla.escalation')">{{ current.escalationLevel || '--' }}</a-descriptions-item>
          <a-descriptions-item :label="t('sla.responseCount')">{{ t('sla.responseUnit', { n: current.responseCount }) }}</a-descriptions-item>
        </a-descriptions>

        <h3 class="drawer-sub"><BellOutlined class="mr-1" />{{ t('sla.remindRecords') }}</h3>
        <a-timeline v-if="(urgeHistory[current.id] || []).length" class="mt-2">
          <a-timeline-item v-for="(h, i) in urgeHistory[current.id]" :key="i" color="orange">
            <div class="font-medium">{{ h.time }}</div>
            <div class="text-[13px]">{{ t('sla.remindWay') }}：{{ h.method }} → {{ h.recipients }}</div>
            <div v-if="h.remark" class="text-[12px] text-[hsl(var(--secondary-text))]">{{ h.remark }}</div>
          </a-timeline-item>
        </a-timeline>
        <div v-else class="text-[13px] text-[hsl(var(--secondary-text))] mt-2">{{ t('sla.noRemind') }}</div>

        <h3 class="drawer-sub">{{ t('sla.escalationTimeline') }}</h3>
        <a-timeline class="mt-2">
          <a-timeline-item v-for="(e, i) in current.escalationTimeline" :key="i" color="blue">
            <div class="font-medium">{{ e.time }}</div>
            <div class="text-[13px]">{{ e.desc }}</div>
            <div class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('sla.notifyTo') }}：{{ e.notifyTo }}</div>
          </a-timeline-item>
        </a-timeline>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button v-if="current && current.slaStatus !== 'responded'" @click="urgeFromDrawer">
            <template #icon><BellOutlined /></template>{{ t('sla.urge') }}
          </a-button>
          <a-button @click="drawerOpen = false">{{ t('common.close') }}</a-button>
        </div>
      </template>
    </a-drawer>

    <!-- 催办 Modal（单个 / 批量） -->
    <a-modal v-model:open="urgeOpen" :title="urgeMode === 'batch' ? t('sla.batchUrgeTitle') : t('sla.urgeTitle')"
      :width="560" :confirm-loading="urgeLoading" @ok="submitUrge">
      <div class="mb-4">
        <div class="fld-label">{{ t('sla.urgeTargetsList') }}</div>
        <div class="target-box">
          <div v-for="id in urgeTargetIds" :key="id">{{ titleOf(id) }}</div>
        </div>
      </div>
      <div class="mb-4">
        <div class="fld-label">{{ t('sla.urgeRecipients') }}</div>
        <a-checkbox-group v-model:value="urgeForm.recipients" class="flex flex-col gap-2">
          <a-checkbox value="publisher">{{ t('sla.target.publisher') }}</a-checkbox>
          <a-checkbox value="supervisor">{{ t('sla.target.supervisor') }}</a-checkbox>
          <a-checkbox value="product_lead">{{ t('sla.target.productLine') }}</a-checkbox>
        </a-checkbox-group>
        <a-select v-if="urgeForm.recipients.includes('product_lead')" :value="urgeForm.productLeadIds"
          mode="multiple" class="w-full mt-2" :placeholder="t('sla.selectProductLead')"
          :options="productLeadOptions" @update:value="(v: any) => (urgeForm.productLeadIds = v)" />
      </div>
      <div class="mb-4">
        <div class="fld-label">{{ t('sla.urgeMethods') }}</div>
        <a-checkbox-group v-model:value="urgeForm.methods" :options="methodOptions" />
        <a-select v-if="urgeForm.methods.includes('email')" :value="urgeForm.emailRecipients"
          mode="tags" class="w-full mt-2" :placeholder="t('sla.emailPlaceholder')"
          :options="emailOptions" @update:value="(v: any) => (urgeForm.emailRecipients = v)" />
      </div>
      <div class="mb-2">
        <div class="fld-label">{{ t('sla.urgeRemark') }}</div>
        <a-textarea v-model:value="urgeForm.remark" :rows="3" :maxlength="200" show-count
          :placeholder="t('common.inputPlaceholder')" />
      </div>

      <div v-if="urgeMode === 'single' && firstTargetId">
        <div class="fld-label">{{ t('sla.urgeHistory') }}</div>
        <div v-if="(urgeHistory[firstTargetId] || []).length" class="history-box">
          <div v-for="(h, i) in urgeHistory[firstTargetId]" :key="i" class="history-item">
            <div class="flex justify-between">
              <span class="font-medium text-[12px]">{{ h.time }}</span>
              <span class="text-[11px] text-[hsl(var(--secondary-text))]">{{ h.operator }}</span>
            </div>
            <div>
              <a-tag color="blue">{{ h.method }}</a-tag><a-tag>→ {{ h.recipients }}</a-tag>
            </div>
          </div>
        </div>
        <div v-else class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('sla.noUrgeHistory') }}</div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import type { Component } from 'vue'
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined, WarningOutlined, ExclamationCircleOutlined, CheckCircleOutlined, BellOutlined } from '@ant-design/icons-vue'
import StatCard from '@/components/stat-card/index.vue'
import { QBigTable } from '@/components/q-big-table'
import type { TableColumn, QBigTableExpose } from '@/components/q-big-table'
import { getSlaList, getSlaStats, getSlaMeta, urgeSlaRequest } from '@/apis/sla/slaApi'
import type { SlaRequestItem, SlaStats, SlaMeta } from '@/apis/sla/types'

defineOptions({ name: 'OperationSla' })
definePage({
  name: 'OperationSla',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.sla.DEFAULT'
  } satisfies RouteMeta
})

const { t } = useI18n()

const urgencyColor: Record<string, string> = { normal: 'default', urgent: 'orange', critical: 'red' }
const slaColor: Record<string, string> = { normal: 'green', warning: 'orange', overdue: 'red', responded: 'blue' }
const slaTextClass: Record<string, string> = {
  normal: 'text-[hsl(var(--success,142_71%_45%))]',
  warning: 'text-[hsl(var(--warning,38_92%_50%))]',
  overdue: 'text-[hsl(var(--error))]',
  responded: 'text-[hsl(var(--primary))]'
}

const allItems = ref<SlaRequestItem[]>([])
const meta = ref<SlaMeta>({ productLeads: [], emailContacts: [] })
const stats = reactive<SlaStats>({ totalRequests: 0, timelyRate: 0, respondedCount: 0, maxOverdueText: '' })

// ============ 实时倒计时 ============
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null
const WINDOW_MS: Record<'critical' | 'urgent' | 'normal', number> = {
  critical: 2 * 3600_000,
  urgent: 4 * 3600_000,
  normal: 24 * 3600_000
}
function formatDur(ms: number): string {
  const totalMin = Math.max(0, Math.floor(ms / 60_000))
  const day = Math.floor(totalMin / 1440)
  if (day > 0) {
    const hour = Math.floor((totalMin % 1440) / 60)
    return `${day}${t('sla.unit.day')} ${hour}${t('sla.unit.hour')}`
  }
  const hour = Math.floor(totalMin / 60)
  const min = totalMin % 60
  return `${hour}${t('sla.unit.hour')} ${min}${t('sla.unit.minute')}`
}
function parseDeadlineTs(s: string): number {
  return new Date(s.replace(' ', 'T')).getTime()
}
function renderRemaining(row: SlaRequestItem): { text: string; cls: string } {
  if (row.slaStatus === 'responded') return { text: t('sla.responded'), cls: slaTextClass.responded ?? '' }
  const remaining = parseDeadlineTs(row.deadline) - now.value
  if (remaining <= 0) return { text: t('sla.overdueBy', { t: formatDur(-remaining) }), cls: slaTextClass.overdue ?? '' }
  const win = WINDOW_MS[row.urgency as keyof typeof WINDOW_MS] ?? WINDOW_MS.normal
  const cls = (remaining < win * 0.3 ? slaTextClass.warning : slaTextClass.normal) ?? ''
  return { text: t('sla.remainingLive', { t: formatDur(remaining) }), cls }
}

// ============ 催办频率限制（demo 10s）============
const URGE_COOLDOWN_MS = 10_000
const urgeCooldownMap = reactive(new Map<string, number>())
function cooldownLeft(id: string): number {
  const until = urgeCooldownMap.get(id)
  if (until === undefined) return 0
  const left = Math.ceil((until - now.value) / 1000)
  return left > 0 ? left : 0
}

// ============ 筛选 ============
const urgencyFilter = ref<string | undefined>(undefined)
const slaStatusFilter = ref<string | undefined>(undefined)
const dateRange = ref<string[]>([])

// ============ 表格选择 ============
const tableRef = ref<QBigTableExpose>()
const selectedRows = ref<SlaRequestItem[]>([])
const selectedIds = computed(() => selectedRows.value.map((r) => r.id))
function onCheckboxChange() {
  selectedRows.value = (tableRef.value?.getCheckboxRecords() ?? []) as SlaRequestItem[]
}
function clearSelection() {
  tableRef.value?.clearCheckboxRow()
  selectedRows.value = []
}

const urgencyOptions = computed(() => [
  { label: t('dict.urgency.critical'), value: 'critical' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.normal'), value: 'normal' }
])
const slaStatusOptions = computed(() => [
  { label: t('dict.slaStatus.normal'), value: 'normal' },
  { label: t('dict.slaStatus.warning'), value: 'warning' },
  { label: t('dict.slaStatus.overdue'), value: 'overdue' },
  { label: t('dict.slaStatus.responded'), value: 'responded' }
])
const methodOptions = computed(() => [
  { label: t('dict.channel.in_app'), value: 'in_app' },
  { label: t('dict.channel.feishu'), value: 'feishu' },
  { label: t('dict.channel.email'), value: 'email' }
])
const productLeadOptions = computed(() =>
  meta.value.productLeads.map((l) => ({ value: l.id, label: `${l.name} · ${l.product}（${l.dept}）` }))
)
const emailOptions = computed(() => meta.value.emailContacts.map((c) => ({ label: c.label, value: c.value })))

const filtered = computed(() => {
  let list = allItems.value
  if (urgencyFilter.value) list = list.filter((r) => r.urgency === urgencyFilter.value)
  if (slaStatusFilter.value) list = list.filter((r) => r.slaStatus === slaStatusFilter.value)
  const [start, end] = dateRange.value || []
  if (start && end) {
    list = list.filter((r) => {
      const d = (r.createdAt || '').slice(0, 10)
      return d >= start && d <= end
    })
  }
  return list
})

const summaryCards = computed<{ key: string; label: string; value: number; color: string; icon: Component }[]>(() => {
  const cnt = (s: string) => allItems.value.filter((r) => r.slaStatus === s).length
  return [
    { key: 'normal', label: t('sla.card.inProgress'), value: cnt('normal'), color: '#1677ff', icon: ClockCircleOutlined },
    { key: 'warning', label: t('sla.card.warning'), value: cnt('warning'), color: '#faad14', icon: WarningOutlined },
    { key: 'overdue', label: t('sla.card.overdue'), value: cnt('overdue'), color: '#ff4d4f', icon: ExclamationCircleOutlined },
    { key: 'responded', label: t('sla.card.responded'), value: cnt('responded'), color: '#52c41a', icon: CheckCircleOutlined }
  ]
})

function renderRowAction(row: SlaRequestItem) {
  return (
    <span class="flex gap-1">
      <a-button type="link" size="small" onClick={() => openDetail(row)}>{t('common.detail')}</a-button>
      {cooldownLeft(row.id) > 0
        ? <a-button type="link" size="small" disabled>{t('sla.urgeCooldown', { s: cooldownLeft(row.id) })}</a-button>
        : <a-button type="link" size="small" onClick={() => openUrge([row.id])}>{t('sla.urge')}</a-button>}
    </span>
  )
}

const columns = computed<TableColumn[]>(() => [
  { field: 'title', title: t('sla.title'), width: 260, slots: { default: ({ row }: any) => <span class="font-medium">{row.title}</span> } },
  {
    field: 'urgency',
    title: t('sla.urgency'),
    width: 90,
    slots: { default: ({ row }: any) => <a-tag color={urgencyColor[row.urgency] ?? 'default'}>{t('dict.urgency.' + row.urgency)}</a-tag> }
  },
  { field: 'createdAt', title: t('sla.createdAt'), width: 160 },
  { field: 'deadline', title: t('sla.deadline'), width: 160 },
  {
    field: 'remaining',
    title: t('sla.remaining'),
    width: 140,
    slots: { default: ({ row }: any) => { const r = renderRemaining(row); return <span class={r.cls}>{r.text}</span> } }
  },
  {
    field: 'slaStatus',
    title: t('common.status'),
    width: 110,
    slots: { default: ({ row }: any) => <a-tag color={slaColor[row.slaStatus] ?? 'default'}>{t('dict.slaStatus.' + row.slaStatus)}</a-tag> }
  },
  { field: 'responseCount', title: t('sla.responseCount'), width: 100, slots: { default: ({ row }: any) => <span>{t('sla.responseUnit', { n: row.responseCount })}</span> } },
  { field: 'action', title: t('common.action'), width: 150, fixed: 'right', slots: { default: ({ row }: any) => renderRowAction(row) } }
])

function onStatusSelect(v: string | undefined) {
  slaStatusFilter.value = v
}
function toggleCard(key: string) {
  slaStatusFilter.value = slaStatusFilter.value === key ? undefined : key
}

// ============ 详情 Drawer ============
const drawerOpen = ref(false)
const current = ref<SlaRequestItem | null>(null)
function openDetail(row: SlaRequestItem) {
  current.value = row
  drawerOpen.value = true
}
function urgeFromDrawer() {
  if (current.value) {
    drawerOpen.value = false
    openUrge([current.value.id])
  }
}

// ============ 催办历史 ============
interface UrgeRecord { time: string; method: string; recipients: string; remark: string; operator: string }
const urgeHistory = reactive<Record<string, UrgeRecord[]>>({})

// ============ 催办 Modal ============
const urgeOpen = ref(false)
const urgeLoading = ref(false)
const urgeMode = ref<'single' | 'batch'>('single')
const urgeTargetIds = ref<string[]>([])
const urgeForm = reactive<{ recipients: string[]; productLeadIds: string[]; methods: string[]; emailRecipients: string[]; remark: string }>({
  recipients: ['publisher'],
  productLeadIds: [],
  methods: ['in_app'],
  emailRecipients: [],
  remark: ''
})
const firstTargetId = computed(() => urgeTargetIds.value[0] ?? '')
function titleOf(id: string): string {
  return allItems.value.find((r) => r.id === id)?.title ?? id
}
function openUrge(ids: string[]) {
  if (!ids.length) return
  urgeMode.value = ids.length > 1 ? 'batch' : 'single'
  urgeTargetIds.value = [...ids]
  urgeForm.recipients = ['publisher']
  urgeForm.productLeadIds = []
  urgeForm.methods = ['in_app']
  urgeForm.emailRecipients = []
  urgeForm.remark = ''
  urgeOpen.value = true
}
async function submitUrge() {
  if (!urgeForm.recipients.length) {
    message.warning(t('sla.recipientRequired'))
    return
  }
  if (!urgeForm.methods.length) {
    message.warning(t('sla.methodRequired'))
    return
  }
  urgeLoading.value = true
  try {
    const first = urgeTargetIds.value[0]
    await urgeSlaRequest({ id: first ?? '', targets: urgeForm.recipients, methods: urgeForm.methods, remark: urgeForm.remark })
    const methodDesc = urgeForm.methods.map((m) => t('dict.channel.' + m)).join('+')
    const recipientDesc = urgeForm.recipients.map((r) => t('sla.target.' + (r === 'product_lead' ? 'productLine' : r === 'supervisor' ? 'supervisor' : 'publisher'))).join('、')
    const stamp = new Date().toTimeString().slice(0, 8)
    urgeTargetIds.value.forEach((id) => {
      const list = urgeHistory[id] ?? []
      list.unshift({ time: stamp, method: methodDesc, recipients: recipientDesc, remark: urgeForm.remark, operator: t('sla.currentUser') })
      urgeHistory[id] = list
      urgeCooldownMap.set(id, Date.now() + URGE_COOLDOWN_MS)
    })
    message.success(urgeMode.value === 'batch' ? t('sla.batchUrgeDone', { n: urgeTargetIds.value.length }) : t('sla.urgeSuccess'))
    urgeOpen.value = false
    clearSelection()
  } finally {
    urgeLoading.value = false
  }
}

async function load() {
  const [listRes, statsRes, metaRes] = await Promise.all([
    getSlaList({ pageNumber: 1, pageSize: 999 }),
    getSlaStats(),
    getSlaMeta()
  ])
  allItems.value = listRes.records
  Object.assign(stats, statsRes)
  meta.value = metaRes
}

onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
  load()
})
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.stat-box {
  padding: 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  background: hsl(var(--card-bg));
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: hsl(var(--primary) / 0.06);
  border-radius: 8px;
  margin-top: 12px;
}
.drawer-sub {
  font-size: 14px;
  font-weight: 600;
  margin-top: 20px;
}
.fld-label {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}
.target-box {
  background: hsl(var(--card-bg));
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  max-height: 90px;
  overflow-y: auto;
}
.history-box {
  max-height: 160px;
  overflow-y: auto;
  background: hsl(var(--card-bg));
  border-radius: 8px;
  padding: 8px 12px;
}
.history-item {
  padding: 6px 0;
  border-bottom: 1px solid hsl(var(--line));
}
.history-item:last-child {
  border-bottom: none;
}
</style>
