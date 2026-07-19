<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col">
    <!-- 统计卡 -->
    <a-row :gutter="16" class="mb-3">
      <a-col :xs="12" :sm="6">
        <div class="stat-card">
          <a-statistic :title="t('sla.stat.total')" :value="stats.totalRequests" />
        </div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-card">
          <a-statistic :title="t('sla.stat.timelyRate')" :value="stats.timelyRate" suffix="%" />
        </div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-card">
          <a-statistic :title="t('sla.stat.responded')" :value="stats.respondedCount" />
        </div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-card">
          <a-statistic :title="t('sla.stat.maxOverdue')" :value="stats.maxOverdueText || '--'" />
        </div>
      </a-col>
    </a-row>

    <div class="flex-1 min-h-0">
      <QBigTable
        ref="tableRef"
        :search-config="searchConfig"
        :columns="columns"
        :query-api="queryApi"
        height="100%"
      />
    </div>

    <!-- 详情 Drawer -->
    <a-drawer v-model:open="drawerOpen" :title="t('sla.detailTitle')" width="480">
      <a-descriptions v-if="current" bordered size="small" :column="1">
        <a-descriptions-item :label="t('sla.title')">{{ current.title }}</a-descriptions-item>
        <a-descriptions-item :label="t('sla.publisher')">{{ current.publisherName ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('sla.urgency')">
          <a-tag :color="urgencyColor[current.urgency] ?? 'default'">{{ t('dict.urgency.' + current.urgency) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('common.status')">
          <a-tag :color="slaColor[current.slaStatus] ?? 'default'">{{ t('dict.slaStatus.' + current.slaStatus) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('sla.createdAt')">{{ current.createdAt ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('sla.deadline')">{{ current.deadline ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('sla.remaining')">
          <span :class="slaTextClass[current.slaStatus]">{{ current.remainingText ?? '--' }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="t('sla.escalation')">{{ current.escalationLevel ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('sla.responseCount')">{{ current.responseCount }}</a-descriptions-item>
      </a-descriptions>
    </a-drawer>

    <!-- 催办 Modal -->
    <a-modal v-model:open="urgeOpen" :title="t('sla.urgeTitle')" :confirm-loading="urgeLoading" @ok="submitUrge">
      <div>
        <div class="mb-4">
          <div class="mb-[6px]">{{ t('sla.urgeTargets') }}</div>
          <a-checkbox-group v-model:value="urgeForm.targets" :options="targetOptions" />
        </div>
        <div class="mb-4">
          <div class="mb-[6px]">{{ t('sla.urgeMethods') }}</div>
          <a-checkbox-group v-model:value="urgeForm.methods" :options="methodOptions" />
        </div>
        <div class="mb-4">
          <div class="mb-[6px]">{{ t('sla.urgeRemark') }}</div>
          <a-textarea v-model:value="urgeForm.remark" :rows="3" :maxlength="200" show-count :placeholder="t('common.inputPlaceholder')" />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { QBigTable } from '@/components/q-big-table'
import type { QBigTableExpose, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { getSlaList, getSlaStats, urgeSlaRequest } from '@/apis/sla/slaApi'
import type { SlaRequestItem, SlaStats } from '@/apis/sla/types'

defineOptions({ name: 'OperationSla' })
definePage({
  name: 'OperationSla',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.sla.DEFAULT'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const tableRef = ref<QBigTableExpose | null>(null)

const urgencyColor: Record<string, string> = { normal: 'default', urgent: 'orange', critical: 'red' }
const slaColor: Record<string, string> = { normal: 'green', warning: 'orange', overdue: 'red', responded: 'blue' }
const slaTextClass: Record<string, string> = {
  normal: 'text-[hsl(var(--success,142_71%_45%))]',
  warning: 'text-[hsl(var(--warning,38_92%_50%))]',
  overdue: 'text-[hsl(var(--error))]',
  responded: 'text-[hsl(var(--primary))]'
}

const stats = reactive<SlaStats>({ totalRequests: 0, timelyRate: 0, respondedCount: 0, maxOverdueText: '' })

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

const searchConfig: FormSchema[] = [
  {
    field: 'urgency',
    label: t('sla.urgency'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: urgencyOptions.value }
  },
  {
    field: 'slaStatus',
    label: t('common.status'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: slaStatusOptions.value }
  },
  {
    field: 'date',
    label: t('sla.createdAt'),
    component: 'RangePicker',
    componentProps: { allowClear: true, style: 'width:240px', valueFormat: 'YYYY-MM-DD' }
  }
]

const columns: TableColumn[] = [
  { field: 'title', title: t('sla.title'), minWidth: 240 },
  {
    field: 'urgency',
    title: t('sla.urgency'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={urgencyColor[row.urgency] ?? 'default'}>{t('dict.urgency.' + row.urgency)}</a-tag> }
  },
  { field: 'createdAt', title: t('sla.createdAt'), width: 160 },
  { field: 'deadline', title: t('sla.deadline'), width: 160 },
  {
    field: 'remainingText',
    title: t('sla.remaining'),
    width: 130,
    slots: { default: ({ row }: any) => <span class={slaTextClass[row.slaStatus] ?? ''}>{row.remainingText ?? '--'}</span> }
  },
  {
    field: 'slaStatus',
    title: t('common.status'),
    width: 110,
    slots: { default: ({ row }: any) => <a-tag color={slaColor[row.slaStatus] ?? 'default'}>{t('dict.slaStatus.' + row.slaStatus)}</a-tag> }
  },
  {
    field: 'responseCount',
    title: t('sla.responseCount'),
    width: 100,
    slots: { default: ({ row }: any) => <span>{t('sla.responseUnit', { n: row.responseCount })}</span> }
  },
  {
    title: t('common.action'),
    width: 140,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => (
        <span class="flex gap-1">
          <a-button type="link" onClick={() => openDetail(row)}>{t('common.detail')}</a-button>
          <a-button type="link" onClick={() => openUrge(row)}>{t('sla.urge')}</a-button>
        </span>
      )
    }
  }
]

// ============ 详情 Drawer ============
const drawerOpen = ref(false)
const current = ref<SlaRequestItem | null>(null)
function openDetail(row: SlaRequestItem) {
  current.value = row
  drawerOpen.value = true
}

// ============ 催办 Modal ============
const urgeOpen = ref(false)
const urgeLoading = ref(false)
const urgeForm = reactive<{ id: string; targets: string[]; methods: string[]; remark: string }>({
  id: '',
  targets: ['publisher'],
  methods: ['in_app'],
  remark: ''
})
const targetOptions = computed(() => [
  { label: t('sla.target.publisher'), value: 'publisher' },
  { label: t('sla.target.supervisor'), value: 'supervisor' },
  { label: t('sla.target.productLine'), value: 'productLine' }
])
const methodOptions = computed(() => [
  { label: t('dict.channel.in_app'), value: 'in_app' },
  { label: t('dict.channel.feishu'), value: 'feishu' },
  { label: t('dict.channel.email'), value: 'email' }
])
function openUrge(row: SlaRequestItem) {
  urgeForm.id = row.id
  urgeForm.targets = ['publisher']
  urgeForm.methods = ['in_app']
  urgeForm.remark = ''
  urgeOpen.value = true
}
async function submitUrge() {
  urgeLoading.value = true
  try {
    await urgeSlaRequest({ ...urgeForm })
    message.success(t('sla.urgeSuccess'))
    urgeOpen.value = false
  } finally {
    urgeLoading.value = false
  }
}

// ============ 列表查询 ============
async function queryApi({ page }: any, searchParams: any) {
  const { date, ...rest } = searchParams ?? {}
  const [startDate, endDate] = Array.isArray(date) ? date : []
  const result = await getSlaList({
    ...rest,
    startDate,
    endDate,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}

onMounted(async () => {
  Object.assign(stats, await getSlaStats())
})
</script>

<style scoped>
.stat-card {
  padding: 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  background: hsl(var(--card-bg));
}
</style>
