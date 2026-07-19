<template>
  <div class="req-square">
    <!-- 标题区 -->
    <div class="page-head">
      <div>
        <div class="page-title">{{ t('requirement.squareTitle') }}</div>
        <div class="page-sub">{{ t('requirement.squareSubtitle') }}</div>
      </div>
      <a-button type="primary" @click="goForm()">
        <template #icon><PlusOutlined /></template>
        {{ t('requirement.add') }}
      </a-button>
    </div>

    <!-- 统计卡 -->
    <div class="stat-row">
      <StatCard :label="t('requirement.statAll')" :value="stats.all" accent="#1677ff" clickable
        :active="statusFilter === ''" @click="setStatus('')">
        <template #icon><InboxOutlined /></template>
      </StatCard>
      <StatCard :label="t('dict.reqStatus.Pending')" :value="stats.pending" accent="#d46b08" clickable
        :active="statusFilter === 'Pending'" @click="setStatus('Pending')">
        <template #icon><ClockCircleOutlined /></template>
      </StatCard>
      <StatCard :label="t('dict.reqStatus.Collecting')" :value="stats.collecting" accent="#2f54eb" clickable
        :active="statusFilter === 'Collecting'" @click="setStatus('Collecting')">
        <template #icon><ThunderboltOutlined /></template>
      </StatCard>
      <StatCard :label="t('dict.reqStatus.Adopted')" :value="stats.adopted" accent="#389e0d" clickable
        :active="statusFilter === 'Adopted'" @click="setStatus('Adopted')">
        <template #icon><CheckCircleOutlined /></template>
      </StatCard>
    </div>

    <!-- 快捷 Tab 行 -->
    <div class="quick-tabs">
      <button v-for="tab in quickTabs" :key="tab.key" class="quick-tab" :class="{ active: quickTab === tab.key }"
        @click="setQuickTab(tab.key)">
        <component :is="tab.icon" />
        <span>{{ tab.label }}</span>
        <span v-if="tab.count !== null" class="tab-count">({{ tab.count }})</span>
      </button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <FilterOutlined class="filter-ic" />
      <a-select class="w-[160px]" :value="categoryFilter" :placeholder="t('requirement.allCategory')" allow-clear
        @update:value="(v: any) => { categoryFilter = v ?? ''; page = 1 }">
        <a-select-option v-for="c in categoryOptions" :key="c" :value="c">{{ c }}</a-select-option>
      </a-select>
      <a-select class="w-[150px]" :value="industryFilter" :placeholder="t('requirement.allIndustry')" allow-clear
        @update:value="(v: any) => { industryFilter = v ?? ''; page = 1 }">
        <a-select-option v-for="i in industryOptions" :key="i" :value="i">{{ i }}</a-select-option>
      </a-select>
      <a-range-picker :value="(dateRange as any)" value-format="YYYY-MM-DD" class="w-[240px]"
        :placeholder="[t('requirement.startDate'), t('requirement.endDate')]"
        @update:value="(v: any) => (dateRange = v || [])" @change="page = 1" />
      <a-select class="w-[140px]" :value="sortKey" @update:value="(v: any) => (sortKey = v)">
        <a-select-option value="latest">{{ t('requirement.sortLatest') }}</a-select-option>
        <a-select-option value="urgency">{{ t('requirement.sortUrgency') }}</a-select-option>
        <a-select-option value="responses">{{ t('requirement.sortResponses') }}</a-select-option>
      </a-select>
      <a-input-search :value="keyword" :placeholder="t('requirement.searchPlaceholder')" allow-clear class="w-[220px]"
        @update:value="(v: any) => (keyword = v ?? '')" @search="page = 1" />
      <div class="view-toggle">
        <button :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'"><BarsOutlined /></button>
        <button :class="{ active: viewMode === 'card' }" @click="viewMode = 'card'"><AppstoreOutlined /></button>
      </div>
    </div>

    <a-spin :spinning="loading">
      <Empty v-if="!filtered.length" type="noData" />

      <!-- 卡片视图 -->
      <div v-else-if="viewMode === 'card'" class="card-grid">
        <div v-for="item in paginated" :key="item.id" class="req-card" @click="goDetail(item.id)">
          <div class="card-cover">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy" />
            <div v-else class="cover-fallback"><InboxOutlined /></div>
            <button class="bookmark-btn" :class="{ active: isBookmarked(item.id) }"
              :title="isBookmarked(item.id) ? t('requirement.unbookmark') : t('requirement.bookmark')"
              @click.stop="toggleBookmark(item.id)">
              <StarFilled v-if="isBookmarked(item.id)" /><StarOutlined v-else />
            </button>
            <a-tag v-if="item.isPinned" color="red" class="pin-badge">{{ t('requirement.pinned') }}</a-tag>
          </div>
          <div class="card-body">
            <div class="card-head-tags">
              <a-tag :color="urgencyColor[item.urgency] || 'default'" class="urgency-tag">
                {{ t('dict.urgency.' + item.urgency) }}
              </a-tag>
              <a-tag :color="statusColor[item.status] || 'default'">{{ t('dict.reqStatus.' + item.status) }}</a-tag>
            </div>
            <div class="card-title">{{ item.title }}</div>
            <div class="card-tags">
              <span class="ind-tag">{{ item.industry }}</span>
              <span v-for="c in item.categoryNames.slice(0, 2)" :key="c" class="cat-tag">{{ c }}</span>
            </div>
            <div class="card-foot">
              <span class="card-author">
                <a-avatar :size="20" class="mini-avatar">{{ firstChar(item.publisherName) }}</a-avatar>
                {{ item.publisherName }}
              </span>
              <span class="card-metrics">
                <span :class="{ hot: item.responseCount > 0 }"><FileTextOutlined /> {{ item.responseCount }}</span>
                <span><EyeOutlined /> {{ fmtCount(item.viewCount) }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="list-view">
        <div v-for="item in paginated" :key="item.id" class="list-row" @click="goDetail(item.id)">
          <button class="row-bookmark" :class="{ active: isBookmarked(item.id) }"
            :title="isBookmarked(item.id) ? t('requirement.unbookmark') : t('requirement.bookmark')"
            @click.stop="toggleBookmark(item.id)">
            <StarFilled v-if="isBookmarked(item.id)" /><StarOutlined v-else />
          </button>
          <div class="row-inner">
            <div class="row-line1">
              <a-tag :color="urgencyColor[item.urgency] || 'default'" class="urgency-tag">
                {{ t('dict.urgency.' + item.urgency) }}
              </a-tag>
              <a-tag v-if="item.isPinned" color="red">{{ t('requirement.pinned') }}</a-tag>
              <span class="row-title">{{ item.title }}</span>
              <a-tag :color="statusColor[item.status] || 'default'">{{ t('dict.reqStatus.' + item.status) }}</a-tag>
            </div>
            <div class="row-line2">
              <span>{{ t('requirement.industry') }}：{{ item.industry }}</span>
              <span class="sep">|</span>
              <span v-for="c in item.categoryNames.slice(0, 3)" :key="c" class="cat-tag">{{ c }}</span>
            </div>
            <div class="row-line3">
              <span class="row-author">
                <a-avatar :size="22" class="mini-avatar">{{ firstChar(item.publisherName) }}</a-avatar>
                {{ item.publisherName }}
                <span class="dept">{{ item.publisherDeptName }}</span>
                <span class="dept">{{ relativeTime(item.createdAt) }}</span>
              </span>
              <span class="card-metrics">
                <span :class="{ hot: item.responseCount > 0 }"><FileTextOutlined /> {{ item.responseCount }}{{ t('requirement.plans') }}</span>
                <span><EyeOutlined /> {{ fmtCount(item.viewCount) }}</span>
              </span>
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
import {
  PlusOutlined, AppstoreOutlined, BarsOutlined, EyeOutlined, FileTextOutlined,
  StarOutlined, StarFilled, InboxOutlined, ClockCircleOutlined, ThunderboltOutlined,
  CheckCircleOutlined, FilterOutlined, UserOutlined, BellOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import StatCard from '@/components/stat-card/index.vue'
import { getRequirementList } from '@/apis/requirement/requirementApi'
import type { RequirementItem } from '@/apis/requirement/types'
import options from '@/apis/requirement/mocks/requirementOptions.json'

defineOptions({ name: 'RequirementList' })
definePage({
  name: 'RequirementList',
  meta: {
    layout: false,
    menu: true,
    title: 'requirement.list'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

// 展示层：本期以固定当前用户模拟「我发布的」；上线后换 SecurityUtils.getCurrentUser
const CURRENT_USER = options.currentUser
const MY_SUBSCRIPTIONS = options.mySubscriptions

const urgencyColor: Record<string, string> = { critical: 'red', urgent: 'orange', normal: 'green' }
const statusColor: Record<string, string> = { Pending: 'orange', Collecting: 'blue', Adopted: 'green', Closed: 'default' }

const allItems = ref<RequirementItem[]>([])
const loading = ref(false)
const keyword = ref('')
const categoryFilter = ref('')
const industryFilter = ref('')
const statusFilter = ref('')
const dateRange = ref<string[]>([])
const sortKey = ref<'latest' | 'urgency' | 'responses'>('latest')
const quickTab = ref('all')
const viewMode = ref<'card' | 'list'>('list')
const page = ref(1)
const pageSize = 9
const bookmarks = ref<string[]>([])

const quickTabs = computed(() => [
  { key: 'all', label: t('requirement.tabAll'), icon: InboxOutlined, count: allItems.value.length },
  { key: 'mine', label: t('requirement.tabMine'), icon: UserOutlined, count: allItems.value.filter((d) => d.publisherName === CURRENT_USER).length },
  { key: 'subscribed', label: t('requirement.tabSubscribed'), icon: BellOutlined, count: allItems.value.filter((d) => d.categoryNames.some((c) => MY_SUBSCRIPTIONS.includes(c))).length },
  { key: 'bookmarked', label: t('requirement.tabBookmarked'), icon: StarOutlined, count: bookmarks.value.length },
  { key: 'latest', label: t('requirement.tabLatest'), icon: ClockCircleOutlined, count: null }
])

const categoryOptions = computed(() => {
  const set = new Set<string>()
  allItems.value.forEach((it) => it.categoryNames.forEach((c) => set.add(c)))
  return [...set]
})
const industryOptions = computed(() => {
  const set = new Set<string>()
  allItems.value.forEach((it) => { if (it.industry) set.add(it.industry) })
  return [...set]
})

const stats = computed(() => ({
  all: allItems.value.length,
  pending: allItems.value.filter((i) => i.status === 'Pending').length,
  collecting: allItems.value.filter((i) => i.status === 'Collecting').length,
  adopted: allItems.value.filter((i) => i.status === 'Adopted').length
}))

const urgencyOrder: Record<string, number> = { critical: 0, urgent: 1, normal: 2 }

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let list = allItems.value.slice()

  if (quickTab.value === 'mine') list = list.filter((d) => d.publisherName === CURRENT_USER)
  else if (quickTab.value === 'subscribed') list = list.filter((d) => d.categoryNames.some((c) => MY_SUBSCRIPTIONS.includes(c)))
  else if (quickTab.value === 'bookmarked') list = list.filter((d) => bookmarks.value.includes(d.id))

  if (kw) list = list.filter((i) => (i.title + i.industry).toLowerCase().includes(kw))
  if (categoryFilter.value) list = list.filter((i) => i.categoryNames.includes(categoryFilter.value))
  if (industryFilter.value) list = list.filter((i) => i.industry === industryFilter.value)
  if (statusFilter.value) list = list.filter((i) => i.status === statusFilter.value)
  if (dateRange.value && dateRange.value[0] && dateRange.value[1]) {
    const start = dateRange.value[0]
    const end = dateRange.value[1]
    list = list.filter((i) => {
      const d = (i.createdAt || '').slice(0, 10)
      return d >= start && d <= end
    })
  }

  const effectiveSort = quickTab.value === 'latest' ? 'latest' : sortKey.value
  const sorted = [...list]
  if (effectiveSort === 'urgency') sorted.sort((a, b) => (urgencyOrder[a.urgency] ?? 9) - (urgencyOrder[b.urgency] ?? 9))
  else if (effectiveSort === 'responses') sorted.sort((a, b) => b.responseCount - a.responseCount)
  else sorted.sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
  // 置顶优先
  sorted.sort((a, b) => (b.isPinned ? 1 : 0) - (a.isPinned ? 1 : 0))
  return sorted
})

const paginated = computed(() =>
  filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)

function fmtCount(n: number): string {
  return n >= 1000 ? (n / 1000).toFixed(1) + 'k' : String(n)
}
function firstChar(name: string): string {
  return name ? name.charAt(0) : '?'
}
function relativeTime(dateStr: string): string {
  if (!dateStr) return '--'
  const time = new Date(dateStr.replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return dateStr
  const day = Math.floor((Date.now() - time) / 86400000)
  if (day < 1) return t('common.today')
  if (day < 30) return t('common.daysAgo', { n: day })
  return dateStr.slice(0, 10)
}

function isBookmarked(id: string): boolean {
  return bookmarks.value.includes(id)
}
function toggleBookmark(id: string) {
  bookmarks.value = bookmarks.value.includes(id)
    ? bookmarks.value.filter((x) => x !== id)
    : [...bookmarks.value, id]
}
function setStatus(s: string) {
  statusFilter.value = s
  page.value = 1
}
function setQuickTab(k: string) {
  quickTab.value = k
  page.value = 1
}
function goDetail(id: string) {
  router.push({ path: '/requirement/detail', query: { id } })
}
function goForm(id?: string) {
  router.push({ path: '/requirement/form', query: id ? { id } : {} })
}

async function load() {
  loading.value = true
  try {
    const res = await getRequirementList({ pageNumber: 1, pageSize: 999 })
    allItems.value = res.records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.req-square {
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
  margin-bottom: 16px;
}
.quick-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.quick-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid hsl(var(--line));
  background: #fff;
  color: hsl(var(--secondary-text));
  transition: all 0.2s;
}
.quick-tab:hover {
  border-color: hsl(var(--primary));
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.06);
}
.quick-tab.active {
  background: hsl(var(--primary));
  color: #fff;
  border-color: hsl(var(--primary));
}
.tab-count {
  font-size: 11px;
  opacity: 0.85;
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
.filter-ic {
  color: hsl(var(--secondary-text));
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
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
}
.req-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.req-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 14px 36px rgba(0, 0, 0, 0.1);
  border-color: hsl(var(--primary) / 0.4);
}
.card-cover {
  height: 120px;
  position: relative;
  overflow: hidden;
  background: hsl(var(--card-bg));
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: hsl(var(--line));
}
.bookmark-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  color: hsl(var(--secondary-text));
  font-size: 15px;
  transition: transform 0.2s;
}
.bookmark-btn:hover {
  transform: scale(1.12);
}
.bookmark-btn.active {
  color: #faad14;
}
.pin-badge {
  position: absolute;
  top: 10px;
  left: 8px;
  margin: 0;
}
.card-body {
  padding: 14px 16px 16px;
}
.card-head-tags {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}
.urgency-tag {
  font-weight: 600;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 45px;
}
.card-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.ind-tag {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  background: hsl(var(--card-bg));
  border-radius: 6px;
  padding: 2px 8px;
}
.cat-tag {
  font-size: 11px;
  color: hsl(var(--secondary-text));
  background: hsl(var(--card-bg));
  border-radius: 6px;
  padding: 2px 6px;
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid hsl(var(--line));
  padding-top: 10px;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.card-author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.mini-avatar {
  background: hsl(var(--primary));
  color: #fff;
  font-size: 10px;
  flex-shrink: 0;
}
.card-metrics {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}
.card-metrics .hot {
  color: hsl(var(--primary));
  font-weight: 600;
}
.list-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.list-row {
  position: relative;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s;
}
.list-row:hover {
  box-shadow: 0 8px 26px rgba(0, 0, 0, 0.08);
  border-color: hsl(var(--primary) / 0.4);
  transform: translateY(-2px);
}
.row-bookmark {
  position: absolute;
  right: 18px;
  top: 16px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 17px;
  color: hsl(var(--line));
  transition: all 0.2s;
}
.row-bookmark:hover,
.row-bookmark.active {
  color: #faad14;
}
.row-inner {
  padding-right: 32px;
}
.row-line1 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.row-title {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-line2 {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-bottom: 8px;
}
.row-line2 .sep {
  color: hsl(var(--line));
}
.row-line3 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.row-author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.row-author .dept {
  color: hsl(var(--secondary-text));
  opacity: 0.8;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
