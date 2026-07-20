<template>
  <div class="opp-browse">
    <!-- 标题区 -->
    <div class="page-head">
      <div>
        <div class="page-title">{{ t('opportunity.browseTitle') }}</div>
        <div class="page-sub">{{ t('opportunity.browseSubtitle') }}</div>
      </div>
      <a-button type="primary" @click="goForm()">
        <template #icon><PlusOutlined /></template>
        {{ t('opportunity.add') }}
      </a-button>
    </div>

    <!-- 统计卡 -->
    <div class="stat-row">
      <StatCard :label="t('opportunity.statAll')" :value="stats.all" accent="#1890ff" clickable
        :active="typeFilter === ''" @click="typeFilter = ''">
        <template #icon><AppstoreOutlined /></template>
      </StatCard>
      <StatCard :label="t('dict.oppType.solution')" :value="stats.solution" accent="#52c41a" clickable
        :active="typeFilter === 'solution'" @click="typeFilter = 'solution'">
        <template #icon><BulbOutlined /></template>
      </StatCard>
      <StatCard :label="t('dict.oppType.success_case')" :value="stats.case" accent="#fa8c16" clickable
        :active="typeFilter === 'success_case'" @click="typeFilter = 'success_case'">
        <template #icon><TrophyOutlined /></template>
      </StatCard>
      <StatCard :label="t('opportunity.statToday')" :value="stats.today" accent="#722ed1">
        <template #icon><ClockCircleOutlined /></template>
      </StatCard>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <a-input-search v-model:value="keyword" :placeholder="t('opportunity.searchPlaceholder')"
        allow-clear class="w-[260px]" @search="page = 1" />
      <a-select :value="typeFilter" @update:value="(v: any) => (typeFilter = v)" class="w-[140px]" :placeholder="t('opportunity.type')" allow-clear>
        <a-select-option value="">{{ t('opportunity.allType') }}</a-select-option>
        <a-select-option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
      </a-select>
      <a-select :value="categoryFilter" @update:value="(v: any) => (categoryFilter = v)" class="w-[160px]" :placeholder="t('opportunity.category')" allow-clear>
        <a-select-option value="">{{ t('opportunity.allCategory') }}</a-select-option>
        <a-select-option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</a-select-option>
      </a-select>
      <a-select :value="sortKey" @update:value="(v: any) => (sortKey = v)" class="w-[130px]">
        <a-select-option value="newest">{{ t('opportunity.sortNewest') }}</a-select-option>
        <a-select-option value="hottest">{{ t('opportunity.sortHottest') }}</a-select-option>
        <a-select-option value="mostLiked">{{ t('opportunity.sortMostLiked') }}</a-select-option>
      </a-select>
      <div class="view-toggle">
        <button :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'"><AppstoreOutlined /></button>
        <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'"><BarsOutlined /></button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <Empty v-if="!filtered.length" type="noData" />

      <!-- 卡片视图 -->
      <div v-else-if="viewMode === 'card'" class="card-grid">
        <div v-for="(item, i) in paginated" :key="item.id" class="opp-card" @click="goDetail(item.id)">
          <div class="card-cover">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy" />
            <div v-else class="cover-fallback"><AppstoreOutlined /></div>
            <span v-if="sortKey === 'hottest' && (page - 1) * pageSize + i < 3" class="rank-badge">
              #{{ (page - 1) * pageSize + i + 1 }}
            </span>
            <a-tag v-if="item.isPinned" color="red" class="pin-badge">{{ t('opportunity.pinned') }}</a-tag>
          </div>
          <div class="card-body">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-summary">{{ item.summary }}</div>
            <div class="card-tags">
              <a-tag :color="typeColor[item.type] || 'default'">{{ t('dict.oppType.' + item.type) }}</a-tag>
              <a-tag v-for="c in item.categoryNames.slice(0, 2)" :key="c">{{ c }}</a-tag>
            </div>
            <div class="card-foot">
              <span class="card-author"><UserOutlined /> {{ item.publisherName }}</span>
              <span class="card-time">{{ relativeTime(item.publishedAt) }}</span>
            </div>
            <SocialStats :views="item.viewCount" :likes="item.likeCount"
              :collects="item.collectCount" :comments="item.commentCount" class="mt-2" />
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="list-view">
        <div v-for="item in paginated" :key="item.id" class="list-row" @click="goDetail(item.id)">
          <div class="list-cover">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy" />
            <div v-else class="cover-fallback sm"><AppstoreOutlined /></div>
          </div>
          <div class="list-main">
            <div class="list-title">
              <a-tag v-if="item.isPinned" color="red">{{ t('opportunity.pinned') }}</a-tag>
              {{ item.title }}
            </div>
            <div class="list-summary">{{ item.summary }}</div>
            <div class="list-meta">
              <a-tag :color="typeColor[item.type] || 'default'">{{ t('dict.oppType.' + item.type) }}</a-tag>
              <span><UserOutlined /> {{ item.publisherName }} · {{ item.publisherDeptName }}</span>
              <span>{{ relativeTime(item.publishedAt) }}</span>
            </div>
          </div>
          <div class="list-side" @click.stop>
            <SocialStats :views="item.viewCount" :likes="item.likeCount"
              :collects="item.collectCount" :comments="item.commentCount" />
            <div class="list-actions">
              <a-button type="link" size="small" @click="goForm(item.id)">{{ t('common.edit') }}</a-button>
              <a-button v-if="item.status === 'published'" type="link" size="small" danger
                @click="handleArchive(item.id)">{{ t('opportunity.archive') }}</a-button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filtered.length" class="pager">
        <a-pagination v-model:current="page" :total="filtered.length" :page-size="pageSize"
          :show-total="(tt: number) => t('common.totalItems', { n: tt })" show-less-items />
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, AppstoreOutlined, BarsOutlined, UserOutlined,
  BulbOutlined, TrophyOutlined, ClockCircleOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import StatCard from '@/components/stat-card/index.vue'
import SocialStats from '@/components/social-stats/index.vue'
import { getOpportunityList, changeOpportunityStatus } from '@/apis/opportunity/opportunityApi'
import type { OpportunityItem } from '@/apis/opportunity/types'

defineOptions({ name: 'OpportunityList' })
definePage({
  name: 'OpportunityList',
  meta: {
    layout: false,
    menu: true,
    title: 'opportunity.list'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const typeColor: Record<string, string> = {
  product_info: 'blue',
  solution: 'green',
  success_case: 'orange'
}

const typeOptions = computed(() => [
  { label: t('dict.oppType.product_info'), value: 'product_info' },
  { label: t('dict.oppType.solution'), value: 'solution' },
  { label: t('dict.oppType.success_case'), value: 'success_case' }
])

const allItems = ref<OpportunityItem[]>([])
const loading = ref(false)
const keyword = ref('')
const typeFilter = ref('')
const categoryFilter = ref('')
const sortKey = ref<'newest' | 'hottest' | 'mostLiked'>('newest')
const viewMode = ref<'card' | 'list'>('card')
const page = ref(1)
const pageSize = 12

const categoryOptions = computed(() => {
  const set = new Set<string>()
  allItems.value.forEach((it) => it.categoryNames.forEach((c) => set.add(c)))
  return [...set]
})

const stats = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return {
    all: allItems.value.length,
    solution: allItems.value.filter((i) => i.type === 'solution').length,
    case: allItems.value.filter((i) => i.type === 'success_case').length,
    today: allItems.value.filter((i) => (i.publishedAt || '').slice(0, 10) === today).length
  }
})

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let list = allItems.value.filter((i) => i.status !== 'draft')
  if (kw) list = list.filter((i) => (i.title + i.summary).toLowerCase().includes(kw))
  if (typeFilter.value) list = list.filter((i) => i.type === typeFilter.value)
  if (categoryFilter.value) list = list.filter((i) => i.categoryNames.includes(categoryFilter.value))
  const sorted = [...list]
  if (sortKey.value === 'hottest') sorted.sort((a, b) => b.viewCount - a.viewCount)
  else if (sortKey.value === 'mostLiked') sorted.sort((a, b) => b.likeCount - a.likeCount)
  else sorted.sort((a, b) => (b.publishedAt || '').localeCompare(a.publishedAt || ''))
  return sorted
})

const paginated = computed(() =>
  filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)

function relativeTime(dateStr: string): string {
  if (!dateStr) return '--'
  const time = new Date(dateStr.replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return dateStr
  const diff = Date.now() - time
  const day = Math.floor(diff / 86400000)
  if (day < 1) return t('common.today')
  if (day < 30) return t('common.daysAgo', { n: day })
  return dateStr.slice(0, 10)
}

function goDetail(id: string) {
  router.push({ path: '/opportunity/detail', query: { id } })
}
function goForm(id?: string) {
  router.push({ path: '/opportunity/form', query: id ? { id } : {} })
}
function handleArchive(id: string) {
  Modal.confirm({
    title: t('opportunity.archiveConfirm'),
    onOk: async () => {
      await changeOpportunityStatus(id, 'archived')
      message.success(t('common.success'))
      await load()
    }
  })
}

async function load() {
  loading.value = true
  try {
    const res = await getOpportunityList({ pageNumber: 1, pageSize: 500 })
    allItems.value = res.records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.opp-browse {
  padding: 16px;
}
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: hsl(var(--text));
}
.page-sub {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin-top: 4px;
}
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 18px;
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
.view-toggle {
  margin-left: auto;
  display: flex;
  gap: 4px;
}
.view-toggle button {
  width: 34px;
  height: 34px;
  border: 1px solid hsl(var(--line));
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  color: hsl(var(--secondary-text));
}
.view-toggle button.active {
  border-color: hsl(var(--primary));
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.06);
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(258px, 1fr));
  gap: 18px;
}
.opp-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.opp-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.1);
  border-color: hsl(var(--primary) / 0.4);
}
.card-cover {
  height: 148px;
  position: relative;
  overflow: hidden;
  background: hsl(var(--card-bg));
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}
.opp-card:hover .card-cover img {
  transform: scale(1.06);
}
.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  color: hsl(var(--line));
}
.cover-fallback.sm {
  font-size: 24px;
}
.rank-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: linear-gradient(135deg, #ff7a45, #ff4d4f);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 9px;
  border-radius: 6px;
}
.pin-badge {
  position: absolute;
  top: 10px;
  right: 2px;
}
.card-body {
  padding: 14px 16px 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 700;
  color: hsl(var(--text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-summary {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  line-height: 1.6;
  margin: 6px 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}
.card-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.list-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.list-row {
  display: flex;
  gap: 16px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
}
.list-row:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.06);
  border-color: hsl(var(--primary) / 0.3);
}
.list-cover {
  width: 120px;
  height: 78px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: hsl(var(--card-bg));
}
.list-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.list-main {
  flex: 1;
  min-width: 0;
}
.list-title {
  font-size: 15px;
  font-weight: 700;
  color: hsl(var(--text));
}
.list-summary {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin: 4px 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.list-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.list-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  flex-shrink: 0;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
