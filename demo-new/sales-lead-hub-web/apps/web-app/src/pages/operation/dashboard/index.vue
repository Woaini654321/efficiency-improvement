<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- 头部：标题 + 更新时间 + 时间范围 + 自定义区间 + 刷新 -->
    <div class="flex items-center justify-between flex-wrap gap-3 mb-4">
      <div class="flex items-center gap-3">
        <h2 class="text-[18px] font-bold">{{ t('dashboard.title') }}</h2>
        <span class="text-[12px] text-[hsl(var(--secondary-text))]">
          {{ t('dashboard.updatedAt') }}：{{ data.updatedAt || '--' }}
        </span>
      </div>
      <div class="flex items-center gap-3 flex-wrap">
        <a-radio-group :value="customActive ? undefined : range" button-style="solid" size="small"
          @change="onRangeRadio">
          <a-radio-button v-for="opt in rangeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
        </a-radio-group>
        <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('dashboard.custom') }}</span>
        <a-range-picker :value="(customRange as any)" size="small" value-format="YYYY-MM-DD"
          class="w-[240px]" :allow-clear="true" @update:value="(v: any) => (customRange = v || [])" />
        <a-button size="small" :loading="loading" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          {{ t('common.refresh') }}
        </a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <!-- Z2: UV / PV 高亮卡（含较上期趋势） -->
      <a-row :gutter="16" class="mb-3">
        <a-col :xs="24" :sm="12">
          <StatCard :label="t('dashboard.uvHint')" :value="fmtNum(data.uv)" accent="#1677ff" :trend="data.uvMom">
            <template #icon><TeamOutlined /></template>
          </StatCard>
        </a-col>
        <a-col :xs="24" :sm="12">
          <StatCard :label="t('dashboard.pvHint')" :value="fmtNum(data.pv)" accent="#722ed1" :trend="data.pvMom">
            <template #icon><EyeOutlined /></template>
          </StatCard>
        </a-col>
      </a-row>

      <!-- Z3: 4 指标卡（含较上期趋势） -->
      <a-row :gutter="16" class="mb-3">
        <a-col v-for="card in metricCards" :key="card.key" :xs="12" :sm="6">
          <StatCard :label="card.title" :value="card.value" :accent="card.accent" :trend="card.trend">
            <template #icon><component :is="card.icon" /></template>
          </StatCard>
        </a-col>
      </a-row>

      <!-- Z4: 趋势折线（周发布量 / 需求响应率） -->
      <a-row :gutter="16" class="mb-3">
        <a-col :xs="24" :lg="12">
          <div class="panel">
            <h3 class="panel-title">{{ t('dashboard.weekPublishTrend') }}</h3>
            <MiniLine :points="trend.pub" :labels="trend.labels" color="#1677ff" gid="dashPubTrend" />
          </div>
        </a-col>
        <a-col :xs="24" :lg="12">
          <div class="panel">
            <h3 class="panel-title">{{ t('dashboard.responseRateTrend') }}</h3>
            <MiniLine :points="trend.rate" :labels="trend.labels" color="#52c41a" gid="dashRateTrend" />
          </div>
        </a-col>
      </a-row>

      <!-- Z5: 内容分类分布饼图 + 页面点击量排行 -->
      <a-row :gutter="16" class="mb-3">
        <a-col :xs="24" :lg="12">
          <div class="panel">
            <div class="flex items-center justify-between mb-3">
              <h3 class="panel-title mb-0">{{ t('dashboard.categoryPie') }}</h3>
              <a-segmented v-model:value="pieType" :options="pieOptions" size="small" />
            </div>
            <MiniPie :segments="pieSegments" :total-label="t('dashboard.pieTotal')" />
          </div>
        </a-col>
        <a-col :xs="24" :lg="12">
          <div class="panel">
            <h3 class="panel-title">{{ t('dashboard.pageClick') }}</h3>
            <MiniBar :bars="pageClickBars" />
          </div>
        </a-col>
      </a-row>

      <!-- Z6: 热门内容 Top5 + 用户活跃时段(8-18点) -->
      <a-row :gutter="16" class="mb-3">
        <a-col :xs="24" :lg="12">
          <div class="panel">
            <h3 class="panel-title"><FireOutlined class="mr-1 text-[hsl(var(--error))]" />{{ t('dashboard.hotTop5') }}</h3>
            <a-list :data-source="data.hotContents.slice(0, 5)" size="small" :split="false">
              <template #renderItem="{ item, index }">
                <a-list-item>
                  <div class="flex items-center gap-2 w-full">
                    <span class="rank" :class="{ 'rank-top': index < 3 }">{{ index + 1 }}</span>
                    <a-tag :color="typeColor[item.type] ?? 'default'">{{ t('dict.oppType.' + item.type) }}</a-tag>
                    <span class="flex-1 truncate">{{ item.title }}</span>
                    <span class="text-[hsl(var(--secondary-text))]">{{ item.viewCount }}</span>
                  </div>
                </a-list-item>
              </template>
            </a-list>
          </div>
        </a-col>
        <a-col :xs="24" :lg="12">
          <div class="panel">
            <h3 class="panel-title">{{ t('dashboard.activeHours') }}</h3>
            <MiniBar :bars="activeHoursBars" />
          </div>
        </a-col>
      </a-row>

      <!-- Z7: 24 小时活跃分布 -->
      <div class="panel">
        <h3 class="panel-title">{{ t('dashboard.hourly24') }}</h3>
        <MiniBar :bars="hours24Bars" />
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import type { Component } from 'vue'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ReloadOutlined, TeamOutlined, EyeOutlined, UserOutlined,
  SendOutlined, AimOutlined, CheckCircleOutlined, FireOutlined
} from '@ant-design/icons-vue'
import StatCard from '@/components/stat-card/index.vue'
import MiniLine from '@/components/mini-chart/line.vue'
import MiniPie from '@/components/mini-chart/pie.vue'
import MiniBar from '@/components/mini-chart/bar.vue'
import { getDashboard } from '@/apis/dashboard/dashboardApi'
import type { DashboardData } from '@/apis/dashboard/types'

defineOptions({ name: 'OperationDashboard' })
definePage({
  name: 'OperationDashboard',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.dashboard.DEFAULT'
  } satisfies RouteMeta
})

const { t } = useI18n()

type RangeKey = 'last7d' | 'last4w' | 'last12w' | 'last6m'

const typeColor: Record<string, string> = {
  product_info: 'blue',
  solution: 'green',
  success_case: 'orange'
}
const CHART_PALETTE = ['#1677ff', '#52c41a', '#faad14', '#722ed1', '#13c2c2', '#eb2f96']

const range = ref<RangeKey>('last12w')
const customRange = ref<string[]>([])
const pieType = ref<string>('opp')
const loading = ref(false)

const rangeOptions = computed(() => [
  { label: t('dashboard.range.last7d'), value: 'last7d' },
  { label: t('dashboard.range.last4w'), value: 'last4w' },
  { label: t('dashboard.range.last12w'), value: 'last12w' },
  { label: t('dashboard.range.last6m'), value: 'last6m' }
])
const pieOptions = computed(() => [
  { label: t('dashboard.pieOpp'), value: 'opp' },
  { label: t('dashboard.pieDemand'), value: 'demand' }
])

const data = reactive<DashboardData>({
  uv: 0,
  pv: 0,
  uvMom: 0,
  pvMom: 0,
  activeUsers: 0,
  activeUsersMom: 0,
  weekPublish: 0,
  weekPublishMom: 0,
  responseRate: 0,
  responseRateMom: 0,
  adoptRate: 0,
  adoptRateMom: 0,
  hotContents: [],
  categoryDist: [],
  pageHeat: [],
  weekPublishTrend: { last7d: [], last4w: [], last12w: [], last6m: [] },
  responseRateTrend: { last7d: [], last4w: [], last12w: [], last6m: [] },
  oppCategoryPie: [],
  demandCategoryPie: [],
  hourlyActive: [],
  updatedAt: ''
})

const metricCards = computed<{ key: string; title: string; value: string; accent: string; trend: number; icon: Component }[]>(() => [
  { key: 'active', title: t('dashboard.activeUsers'), value: String(data.activeUsers), accent: '#1677ff', trend: data.activeUsersMom, icon: UserOutlined },
  { key: 'week', title: t('dashboard.weekPublish'), value: String(data.weekPublish), accent: '#52c41a', trend: data.weekPublishMom, icon: SendOutlined },
  { key: 'resp', title: t('dashboard.responseRate'), value: data.responseRate + '%', accent: '#faad14', trend: data.responseRateMom, icon: AimOutlined },
  { key: 'adopt', title: t('dashboard.adoptRate'), value: data.adoptRate + '%', accent: '#722ed1', trend: data.adoptRateMom, icon: CheckCircleOutlined }
])

const customActive = computed(() => {
  const r = customRange.value || []
  return Array.isArray(r) && r.length === 2 && !!r[0] && !!r[1]
})

function fmtNum(n: number): string {
  return n.toLocaleString('en-US')
}

function dayLabels(count: number): string[] {
  const out: string[] = []
  const today = new Date()
  for (let i = count - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    out.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return out
}
function weekLabels(count: number): string[] {
  const out: string[] = []
  const today = new Date()
  const dow = today.getDay()
  const mondayOffset = dow === 0 ? 6 : dow - 1
  const thisMonday = new Date(today)
  thisMonday.setDate(today.getDate() - mondayOffset)
  for (let i = count - 1; i >= 0; i--) {
    const ws = new Date(thisMonday)
    ws.setDate(thisMonday.getDate() - i * 7)
    const lbl = `${ws.getMonth() + 1}/${ws.getDate()}`
    out.push(count > 12 ? (i % 3 === 0 ? lbl : '') : lbl)
  }
  return out
}

// 自定义区间：确定性生成 mock 序列（展示层占位，非真实统计）
function genCustom(): { pub: number[]; rate: number[]; labels: string[] } {
  const r = customRange.value || []
  const start = r[0]
  const end = r[1]
  if (!start || !end) return { pub: [], rate: [], labels: [] }
  const days = Math.floor((new Date(end).getTime() - new Date(start).getTime()) / 86400000) + 1
  const weeks = Math.min(26, Math.max(2, Math.ceil(days / 7)))
  let seed = new Date(start).getTime() % 1000
  const pub: number[] = []
  const rate: number[] = []
  const labels: string[] = []
  const endDate = new Date(end)
  for (let i = 0; i < weeks; i++) {
    seed = (seed * 16807) % 2147483647
    pub.push((seed % 18) + 10)
    seed = (seed * 16807) % 2147483647
    rate.push((seed % 20) + 60)
  }
  for (let i = weeks - 1; i >= 0; i--) {
    const d = new Date(endDate)
    d.setDate(endDate.getDate() - i * 7)
    labels.push(weeks > 12 ? (i % 3 === 0 ? `${d.getMonth() + 1}/${d.getDate()}` : '') : `${d.getMonth() + 1}/${d.getDate()}`)
  }
  return { pub, rate, labels }
}

const trend = computed<{ pub: number[]; rate: number[]; labels: string[] }>(() => {
  if (customActive.value) return genCustom()
  const k = range.value
  const pub = data.weekPublishTrend[k]
  const rate = data.responseRateTrend[k]
  const labels = k === 'last7d' ? dayLabels(pub.length) : weekLabels(pub.length)
  return { pub, rate, labels }
})

const pieSegments = computed(() =>
  pieType.value === 'opp' ? data.oppCategoryPie : data.demandCategoryPie
)

const pageClickBars = computed(() =>
  data.pageHeat.map((p, i) => ({ label: p.page, value: p.count, color: CHART_PALETTE[i % CHART_PALETTE.length] ?? '#1677ff' }))
)

function heatColor(v: number, mx: number): string {
  if (v > mx * 0.6) return '#1677ff'
  if (v > mx * 0.3) return '#52c41a'
  return '#d9d9d9'
}
const activeHoursBars = computed(() => {
  const hrs = [8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18]
  const vals = hrs.map((h) => data.hourlyActive[h] ?? 0)
  const mx = Math.max(1, ...vals)
  return hrs.map((h, idx) => {
    const v = vals[idx] ?? 0
    return { label: `${h}:00`, value: v, color: heatColor(v, mx) }
  })
})
const hours24Bars = computed(() => {
  const mx = Math.max(1, ...data.hourlyActive)
  return data.hourlyActive.map((v, h) => ({ label: `${h}:00`, value: v, color: heatColor(v, mx) }))
})

function onRangeRadio(e: any) {
  range.value = e.target.value as RangeKey
  customRange.value = []
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDashboard({ range: range.value })
    Object.assign(data, res)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.panel {
  padding: 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  margin-bottom: 12px;
  background: hsl(var(--card-bg));
  min-height: 180px;
}
.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}
.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  font-size: 12px;
  background: hsl(var(--secondary));
  color: hsl(var(--secondary-text));
}
.rank-top {
  background: hsl(var(--primary));
  color: #fff;
}
</style>
