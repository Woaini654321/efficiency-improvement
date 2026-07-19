<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- 头部：标题 + 时间范围 + 刷新 -->
    <div class="flex items-center justify-between flex-wrap gap-3 mb-4">
      <div class="flex items-center gap-3">
        <h2 class="text-[18px] font-bold">{{ t('dashboard.title') }}</h2>
        <span class="text-[12px] text-[hsl(var(--secondary-text))]">
          {{ t('dashboard.updatedAt') }}：{{ data.updatedAt || '--' }}
        </span>
      </div>
      <div class="flex items-center gap-3">
        <a-radio-group v-model:value="range" button-style="solid" size="small" @change="loadData">
          <a-radio-button v-for="opt in rangeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</a-radio-button>
        </a-radio-group>
        <a-button size="small" :loading="loading" @click="loadData">{{ t('common.refresh') }}</a-button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <!-- UV / PV 双卡 -->
      <a-row :gutter="16" class="mb-3">
        <a-col :xs="24" :sm="12">
          <div class="stat-card">
            <a-statistic :title="t('dashboard.uv')" :value="data.uv" />
            <div class="mom" :class="momClass(data.uvMom)">{{ momText(data.uvMom) }}</div>
          </div>
        </a-col>
        <a-col :xs="24" :sm="12">
          <div class="stat-card">
            <a-statistic :title="t('dashboard.pv')" :value="data.pv" />
            <div class="mom" :class="momClass(data.pvMom)">{{ momText(data.pvMom) }}</div>
          </div>
        </a-col>
      </a-row>

      <!-- 4 统计卡 -->
      <a-row :gutter="16" class="mb-3">
        <a-col v-for="card in metricCards" :key="card.key" :xs="12" :sm="6">
          <div class="stat-card">
            <a-statistic :title="card.title" :value="card.value" :suffix="card.suffix" />
            <div class="mom" :class="momClass(card.mom)">{{ momText(card.mom) }}</div>
          </div>
        </a-col>
      </a-row>

      <!-- 分布区：热门内容 / 分类分布 / 页面热度 -->
      <a-row :gutter="16">
        <a-col :xs="24" :lg="8">
          <div class="panel">
            <h3 class="panel-title">{{ t('dashboard.hotContents') }}</h3>
            <a-list :data-source="data.hotContents" size="small" :split="false">
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

        <a-col :xs="24" :lg="8">
          <div class="panel">
            <h3 class="panel-title">{{ t('dashboard.categoryDist') }}</h3>
            <div v-for="c in data.categoryDist" :key="c.name" class="dist-row">
              <div class="flex justify-between mb-1">
                <span>{{ c.name }}</span>
                <span class="text-[hsl(var(--secondary-text))]">{{ c.count }} · {{ c.percent }}%</span>
              </div>
              <a-progress :percent="c.percent" :show-info="false" size="small" />
            </div>
          </div>
        </a-col>

        <a-col :xs="24" :lg="8">
          <div class="panel">
            <h3 class="panel-title">{{ t('dashboard.pageHeat') }}</h3>
            <div v-for="p in data.pageHeat" :key="p.page" class="dist-row">
              <div class="flex justify-between mb-1">
                <span>{{ p.page }}</span>
                <span class="text-[hsl(var(--secondary-text))]">{{ p.count }}</span>
              </div>
              <a-progress :percent="p.percent" :show-info="false" size="small" stroke-color="hsl(var(--primary))" />
            </div>
          </div>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getDashboard } from '@/apis/dashboard/dashboardApi'
import type { DashboardData } from '@/apis/dashboard/types'

defineOptions({ name: 'OperationDashboard' })
definePage({
  name: 'OperationDashboard',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.dashboard.DEFAULT'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()

const typeColor: Record<string, string> = {
  product_info: 'blue',
  solution: 'green',
  success_case: 'orange'
}

const range = ref('last7d')
const loading = ref(false)
const rangeOptions = computed(() => [
  { label: t('dashboard.range.last7d'), value: 'last7d' },
  { label: t('dashboard.range.last4w'), value: 'last4w' },
  { label: t('dashboard.range.last12w'), value: 'last12w' },
  { label: t('dashboard.range.last6m'), value: 'last6m' }
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
  updatedAt: ''
})

const metricCards = computed(() => [
  { key: 'active', title: t('dashboard.activeUsers'), value: data.activeUsers, suffix: '', mom: data.activeUsersMom },
  { key: 'week', title: t('dashboard.weekPublish'), value: data.weekPublish, suffix: '', mom: data.weekPublishMom },
  { key: 'resp', title: t('dashboard.responseRate'), value: data.responseRate, suffix: '%', mom: data.responseRateMom },
  { key: 'adopt', title: t('dashboard.adoptRate'), value: data.adoptRate, suffix: '%', mom: data.adoptRateMom }
])

function momClass(v: number) {
  return v >= 0 ? 'mom-up' : 'mom-down'
}
function momText(v: number) {
  const sign = v >= 0 ? '+' : ''
  return `${t('dashboard.mom')} ${sign}${v}%`
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
.stat-card {
  padding: 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  background: hsl(var(--card-bg));
}

.mom {
  margin-top: 4px;
  font-size: 12px;
}

.mom-up {
  color: hsl(var(--success, 142 71% 45%));
}

.mom-down {
  color: hsl(var(--error));
}

.panel {
  padding: 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  margin-top: 12px;
  min-height: 220px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}

.dist-row {
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
