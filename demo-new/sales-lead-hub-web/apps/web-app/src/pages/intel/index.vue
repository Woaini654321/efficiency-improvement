<template>
  <div class="intel-center">
    <!-- 标题区 -->
    <div class="page-head">
      <div class="page-title"><RadarChartOutlined /> {{ t('intel.centerTitle') }}</div>
    </div>

    <a-segmented :value="activeTab" :options="tabOptions" class="mb-4"
      @update:value="(v: any) => (activeTab = v)" />

    <a-spin :spinning="loading">
      <!-- ============ 竞品情报 ============ -->
      <div v-if="activeTab === 'competitor'">
        <div class="filter-bar">
          <a-select :value="compBrand" class="w-[170px]" :placeholder="t('intel.brand')" allow-clear
            @update:value="(v: any) => (compBrand = v || '')">
            <a-select-option v-for="b in brandOptions" :key="b" :value="b">{{ b }}</a-select-option>
          </a-select>
          <a-select :value="compType" class="w-[150px]" :placeholder="t('intel.intelType')" allow-clear
            @update:value="(v: any) => (compType = v || '')">
            <a-select-option v-for="o in intelTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
          </a-select>
          <a-range-picker :value="(compRange as any)" value-format="YYYY-MM-DD"
            :placeholder="[t('intel.rangeStart'), t('intel.rangeEnd')]" class="w-[240px]"
            @update:value="(v: any) => (compRange = v || [])" />
          <a-button type="primary" class="ml-auto" @click="goSubmit">
            <template #icon><PlusOutlined /></template>
            {{ t('intel.submit') }}
          </a-button>
        </div>

        <Empty v-if="!filteredCompetitors.length" type="noData" />
        <div v-else class="comp-list">
          <div v-for="c in filteredCompetitors" :key="c.id" class="intel-card" @click="goCompetitor(c.id)">
            <div class="intel-card-head">
              <a-tag color="purple">{{ c.brand }}</a-tag>
              <a-tag v-if="c.product">{{ c.product }}</a-tag>
              <a-tag :color="intelTypeColor[c.intelType] || 'default'">{{ t('dict.intelType.' + c.intelType) }}</a-tag>
              <span class="intel-card-title">{{ c.title }}</span>
            </div>
            <div class="intel-card-summary">{{ c.summary }}</div>
            <div class="intel-card-foot">
              <span>{{ t('intel.source') }}: {{ c.source }} · {{ t('intel.submitter') }}: {{ c.submitterName }}</span>
              <span class="flex items-center gap-3">
                <SocialStats :views="c.viewCount" :likes="c.likeCount" :collects="c.collectCount" />
                <span>{{ (c.createdAt || '').slice(0, 10) }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- ============ 行业情报 ============ -->
      <div v-else>
        <div class="filter-bar">
          <a-select :value="industryFilter" class="w-[170px]" :placeholder="t('intel.industry')" allow-clear
            @update:value="(v: any) => (industryFilter = v || '')">
            <a-select-option v-for="o in industryOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
          </a-select>
          <a-range-picker :value="(indRange as any)" value-format="YYYY-MM-DD"
            :placeholder="[t('intel.rangeStart'), t('intel.rangeEnd')]" class="w-[240px]"
            @update:value="(v: any) => (indRange = v || [])" />
          <a-input-search v-model:value="industryKeyword" :placeholder="t('intel.searchIndustry')"
            allow-clear class="w-[240px]" />
        </div>

        <Empty v-if="!filteredIndustries.length" type="noData" />
        <div v-else class="industry-grid">
          <div v-for="item in filteredIndustries" :key="item.id" class="industry-card" @click="goIndustry(item.id)">
            <div class="industry-card-img"><component :is="industryIcon[item.industry] || GlobalOutlined" /></div>
            <a-tag color="blue" class="mb-2">{{ t('intel.industryDict.' + item.industry) }}</a-tag>
            <h4 class="industry-card-title">{{ item.title }}</h4>
            <p class="industry-card-desc">{{ item.summary }}</p>
            <div class="industry-card-foot">
              <span>{{ t('intel.source') }}: {{ item.source }}</span>
              <span>{{ (item.createdAt || '').slice(0, 10) }}</span>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import type { Component } from 'vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  RadarChartOutlined, PlusOutlined, GlobalOutlined, RiseOutlined, CarOutlined,
  SafetyCertificateOutlined, ThunderboltOutlined, BuildOutlined, ClusterOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import SocialStats from '@/components/social-stats/index.vue'
import { getCompetitorList, getIndustryList } from '@/apis/intel/intelApi'
import type { CompetitorIntelItem, IndustryIntelItem } from '@/apis/intel/types'

defineOptions({ name: 'IntelList' })
definePage({
  name: 'IntelList',
  meta: {
    layout: false,
    menu: true,
    title: 'intel.list'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const activeTab = ref<'competitor' | 'industry'>('competitor')

const tabOptions = computed(() => [
  { label: t('intel.tabCompetitor'), value: 'competitor' },
  { label: t('intel.tabIndustry'), value: 'industry' }
])

const intelTypeColor: Record<string, string> = {
  new_product: 'green',
  price_change: 'orange',
  customer_case: 'blue',
  other: 'default'
}
const industryIcon: Record<string, Component> = {
  trend: RiseOutlined,
  automotive: CarOutlined,
  policy: SafetyCertificateOutlined,
  energy: ThunderboltOutlined,
  industrial: BuildOutlined,
  smartcity: ClusterOutlined
}

const brandOptions = ['Sierra Wireless', 'Telit', '广和通', '移远', 'u-blox', 'Thales']
const intelTypeOptions = computed(() => [
  { label: t('dict.intelType.new_product'), value: 'new_product' },
  { label: t('dict.intelType.price_change'), value: 'price_change' },
  { label: t('dict.intelType.customer_case'), value: 'customer_case' },
  { label: t('dict.intelType.other'), value: 'other' }
])
const industryOptions = computed(() => [
  { label: t('intel.industryDict.trend'), value: 'trend' },
  { label: t('intel.industryDict.automotive'), value: 'automotive' },
  { label: t('intel.industryDict.policy'), value: 'policy' },
  { label: t('intel.industryDict.energy'), value: 'energy' },
  { label: t('intel.industryDict.industrial'), value: 'industrial' },
  { label: t('intel.industryDict.smartcity'), value: 'smartcity' }
])

const loading = ref(false)
const competitors = ref<CompetitorIntelItem[]>([])
const industries = ref<IndustryIntelItem[]>([])

// 竞品筛选
const compBrand = ref('')
const compType = ref('')
const compRange = ref<string[]>([])
// 行业筛选
const industryFilter = ref('')
const industryKeyword = ref('')
const indRange = ref<string[]>([])

function inRange(dateStr: string, range: string[]): boolean {
  if (!range || !range[0] || !range[1]) return true
  const d = (dateStr || '').slice(0, 10)
  return d >= range[0] && d <= range[1]
}

const filteredCompetitors = computed(() =>
  competitors.value.filter((c) => {
    if (compBrand.value && c.brand !== compBrand.value) return false
    if (compType.value && c.intelType !== compType.value) return false
    return inRange(c.createdAt, compRange.value)
  })
)

const filteredIndustries = computed(() => {
  const kw = industryKeyword.value.trim().toLowerCase()
  return industries.value.filter((item) => {
    if (industryFilter.value && item.industry !== industryFilter.value) return false
    if (!inRange(item.createdAt, indRange.value)) return false
    if (kw && !(item.title + item.summary).toLowerCase().includes(kw)) return false
    return true
  })
})

function goCompetitor(id: string) {
  router.push({ path: '/intel/competitor', query: { id } })
}
function goIndustry(id: string) {
  router.push({ path: '/intel/industry', query: { id } })
}
function goSubmit() {
  router.push({ path: '/intel/submit' })
}

onMounted(async () => {
  loading.value = true
  try {
    const [comp, ind] = await Promise.all([
      getCompetitorList({ pageNumber: 1, pageSize: 999 }),
      getIndustryList({ pageNumber: 1, pageSize: 999 })
    ])
    competitors.value = comp.records
    industries.value = ind.records
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.intel-center {
  padding: 16px;
}
.page-head {
  margin-bottom: 16px;
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: hsl(var(--text));
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  margin-bottom: 18px;
}
/* 竞品情报卡片流 */
.comp-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.intel-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  border: 1px solid hsl(var(--line));
  cursor: pointer;
  transition: all 0.25s;
}
.intel-card:hover {
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
  border-color: hsl(var(--primary) / 0.4);
  transform: translateY(-2px);
}
.intel-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.intel-card-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  flex: 1;
  min-width: 200px;
}
.intel-card-summary {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.intel-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: hsl(var(--secondary-text));
  flex-wrap: wrap;
  gap: 8px;
}
/* 行业情报网格卡 */
.industry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}
.industry-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid hsl(var(--line));
  cursor: pointer;
  transition: all 0.25s;
}
.industry-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  border-color: hsl(var(--primary) / 0.4);
}
.industry-card-img {
  width: 100%;
  height: 120px;
  border-radius: 10px;
  background: linear-gradient(135deg, hsl(var(--primary) / 0.1), hsl(var(--primary) / 0.04));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  font-size: 40px;
  color: hsl(var(--primary));
}
.industry-card-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 6px;
  line-height: 1.4;
}
.industry-card-desc {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.industry-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
</style>
