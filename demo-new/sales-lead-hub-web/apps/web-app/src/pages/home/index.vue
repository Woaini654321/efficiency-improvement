<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- Hero 搜索区 -->
    <div class="hero rounded-[8px] px-[24px] py-[28px] mb-4">
      <span class="hero-badge">
        <span class="live-dot"></span>
        {{ t('home.heroBadge') }}
      </span>
      <h1 class="text-[22px] font-bold text-white mb-1">{{ t('home.heroTitle') }}</h1>
      <p class="text-white/80 mb-4">{{ t('home.heroSubtitle') }}</p>
      <a-input-search
        v-model:value="keyword"
        :placeholder="t('home.searchPlaceholder')"
        enter-button
        size="large"
        class="max-w-[560px]"
        @search="onSearch"
      />
      <div class="mt-3 flex items-center flex-wrap gap-2">
        <span class="text-white/70">{{ t('home.hotSearch') }}</span>
        <a-tag
          v-for="tag in dashboard?.hotTags ?? []"
          :key="tag"
          class="cursor-pointer"
          @click="onSearch(tag)"
          >{{ tag }}</a-tag
        >
      </div>
    </div>

    <!-- 4 统计卡 -->
    <a-row :gutter="16" class="mb-4">
      <a-col v-for="s in statCards" :key="s.key" :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="false" class="stat-card">
          <div class="text-[26px] font-bold text-[hsl(var(--primary))]">{{ s.value }}</div>
          <div class="text-[hsl(var(--secondary-text))] mt-1">{{ s.label }}</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 快捷操作 4 卡 -->
    <div class="text-[15px] font-semibold mb-2">{{ t('home.quickActions') }}</div>
    <a-row :gutter="16" class="mb-4">
      <a-col v-for="q in quickActions" :key="q.key" :xs="12" :sm="12" :md="6">
        <a-card hoverable size="small" class="mb-2" @click="router.push(q.path)">
          <div class="flex items-center gap-2">
            <span class="text-[22px]">{{ q.emoji }}</span>
            <div>
              <div class="font-medium">{{ q.title }}</div>
              <div class="text-[12px] text-[hsl(var(--secondary-text))]">{{ q.desc }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <!-- 我的任务看板 -->
      <a-col :xs="24" :md="14">
        <a-card size="small" class="mb-4">
          <template #title>
            <span>{{ t('home.taskBoard') }}</span>
            <a-tag color="blue" class="ml-2">{{ t('home.pending') }} {{ pendingCount }}</a-tag>
            <a-tag color="red">{{ t('home.overdue') }} {{ overdueCount }}</a-tag>
          </template>
          <template #extra>
            <a class="text-[hsl(var(--primary))]" @click="router.push('/task')">{{ t('home.viewAll') }}</a>
          </template>
          <a-list :data-source="dashboard?.quickTasks ?? []" :locale="{ emptyText: t('home.noTasks') }">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="w-full flex items-center justify-between gap-2">
                  <div class="flex items-center gap-2 min-w-0">
                    <span class="dot" :class="'dot-' + item.priority"></span>
                    <span class="truncate">{{ item.title }}</span>
                    <span class="text-[12px] text-[hsl(var(--secondary-text))] shrink-0">{{ item.meetingName }}</span>
                  </div>
                  <div class="flex items-center gap-1 shrink-0">
                    <span v-if="item.isOverdue" class="overdue-dot pulse shrink-0"></span>
                    <span
                      class="text-[12px]"
                      :class="item.isOverdue ? 'text-[hsl(var(--error))] font-semibold pulse' : 'text-[hsl(var(--secondary-text))]'"
                      >{{ item.deadline }}</span
                    >
                    <a-tag :color="priorityColor[item.priority] ?? 'default'">{{ t('dict.urgency.' + item.priority) }}</a-tag>
                    <a-tag :color="taskStatusColor[item.status] ?? 'default'">{{ t('dict.taskStatus.' + item.status) }}</a-tag>
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <!-- 热门方案 -->
        <a-card size="small" class="mb-4">
          <template #title>{{ t('home.hotSolutions') }}</template>
          <template #extra>
            <a class="text-[hsl(var(--primary))]" @click="router.push('/opportunity')">{{ t('home.browseAll') }}</a>
          </template>
          <a-row :gutter="12">
            <a-col v-for="sol in dashboard?.hotSolutions ?? []" :key="sol.id" :xs="24" :sm="12">
              <div
                class="sol-item p-2 rounded cursor-pointer mb-2"
                @click="router.push({ path: '/opportunity/detail', query: { id: sol.id } })"
              >
                <div class="flex items-center gap-1 mb-1">
                  <span class="rank">#{{ sol.rank }}</span>
                  <a-tag :color="typeColor[sol.type] ?? 'default'">{{ t('dict.oppType.' + sol.type) }}</a-tag>
                  <a-tag v-if="sol.isSubscribed" color="green">{{ t('home.subscribed') }}</a-tag>
                </div>
                <div class="truncate font-medium">{{ sol.title }}</div>
                <div class="text-[12px] text-[hsl(var(--secondary-text))] mt-1">
                  {{ sol.publisherName }} · {{ t('common.viewCount') }} {{ sol.viewCount }}
                </div>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>

      <!-- 右栏：公告 + 讨论热帖 -->
      <a-col :xs="24" :md="10">
        <a-card size="small" class="mb-4">
          <template #title>{{ t('home.announcements') }}</template>
          <a-list :data-source="dashboard?.announcements ?? []">
            <template #renderItem="{ item }">
              <a-list-item>
                <div
                  class="w-full min-w-0 list-link p-2 rounded cursor-pointer"
                  @click="router.push({ path: '/notification/announcement', query: { id: item.id } })"
                >
                  <div class="flex items-center gap-1 mb-1">
                    <a-tag v-if="item.isPinned" color="red">{{ t('opportunity.pinned') }}</a-tag>
                    <a-tag :color="announceColor[item.type] ?? 'default'">{{ t('dict.announceType.' + item.type) }}</a-tag>
                    <span class="truncate font-medium">{{ item.title }}</span>
                  </div>
                  <div class="text-[12px] text-[hsl(var(--secondary-text))]">
                    {{ item.publisherName }} · {{ item.publishedAt }} · {{ t('common.viewCount') }} {{ item.viewCount }}
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <a-card size="small" class="mb-4">
          <template #title>{{ t('home.hotPosts') }}</template>
          <template #extra>
            <a class="text-[hsl(var(--primary))]" @click="router.push('/discussion')">{{ t('home.viewAll') }}</a>
          </template>
          <a-list :data-source="dashboard?.hotPosts ?? []">
            <template #renderItem="{ item }">
              <a-list-item>
                <div
                  class="w-full min-w-0 list-link p-2 rounded cursor-pointer"
                  @click="router.push({ path: '/discussion/detail', query: { id: item.id } })"
                >
                  <div class="flex items-center gap-1 mb-1">
                    <a-tag :color="topicColor[item.topic] ?? 'default'">{{ t('home.topic.' + item.topic) }}</a-tag>
                    <span class="truncate font-medium">{{ item.title }}</span>
                  </div>
                  <div class="text-[12px] text-[hsl(var(--secondary-text))]">
                    {{ item.authorName }} · {{ t('home.replies') }} {{ item.replyCount }} ·
                    {{ t('common.viewCount') }} {{ item.viewCount }} · {{ item.createdAt }}
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>

    <!-- 情报中心面板（数据引用 apis/intel mock，SSOT）-->
    <div class="intel-panel mb-4">
      <div class="intel-header">
        <div class="intel-header-info">
          <div class="intel-header-icon"><RadarChartOutlined /></div>
          <div>
            <div class="intel-header-title">{{ t('home.intel.title') }}</div>
            <div class="intel-header-desc">{{ t('home.intel.desc') }}</div>
          </div>
        </div>
        <a class="intel-enter" @click="router.push('/intel')">
          {{ t('home.intel.enter') }} <ArrowRightOutlined />
        </a>
      </div>
      <div class="intel-body">
        <div v-if="!intelItems.length" class="intel-empty">
          <Empty type="noData" />
        </div>
        <div
          v-for="item in intelItems"
          :key="item.id"
          class="intel-item"
          @click="router.push('/intel')"
        >
          <div class="intel-brand" :class="'brand-' + item.tone">{{ item.badge }}</div>
          <div class="intel-main">
            <div class="intel-title">{{ item.title }}</div>
            <div class="intel-meta">
              <span class="intel-type" :class="'type-' + item.tone">{{ item.typeLabel }}</span>
              <span class="intel-time">{{ relativeTime(item.createdAt) }}</span>
            </div>
          </div>
          <RightOutlined class="intel-arrow" />
        </div>
      </div>
      <div class="intel-links">
        <a class="intel-link" @click="router.push('/intel')">
          <StockOutlined /> {{ t('home.intel.linkCompetitor') }}
        </a>
        <a class="intel-link" @click="router.push('/intel')">
          <RiseOutlined /> {{ t('home.intel.linkIndustry') }}
        </a>
        <a class="intel-link" @click="router.push('/intel/submit')">
          <EditOutlined /> {{ t('home.intel.linkSubmit') }}
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  RadarChartOutlined, ArrowRightOutlined, RightOutlined,
  StockOutlined, RiseOutlined, EditOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import { getHomeDashboard } from '@/apis/home/homeApi'
import type { HomeDashboard } from '@/apis/home/types'
import { getCompetitorList, getIndustryList } from '@/apis/intel/intelApi'

defineOptions({ name: 'HomeDashboard' })
definePage({
  name: 'HomeDashboard',
  meta: {
    layout: 'default',
    menu: { icon: 'q-icon:home-linear' },
    title: 'home'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const keyword = ref('')
const dashboard = ref<HomeDashboard | null>(null)

const priorityColor: Record<string, string> = { critical: 'red', urgent: 'orange', normal: 'default' }
const taskStatusColor: Record<string, string> = { pending: 'blue', processing: 'orange', completed: 'green', transferred: 'default', cancelled: 'default' }
const typeColor: Record<string, string> = { product_info: 'blue', solution: 'green', success_case: 'orange' }
const announceColor: Record<string, string> = { notice: 'blue', policy: 'default', activity: 'green', other: 'default' }
const topicColor: Record<string, string> = { business: 'blue', solution: 'orange', experience: 'green', industry: 'default', complaint: 'red' }

const STAT_KEYS = ['solutionTotal', 'pendingRequests', 'weekDiscussions', 'activeUsers'] as const
type StatKey = (typeof STAT_KEYS)[number]

// count-up-on-mount micro animation：数值从 0 缓动到目标值
const animatedStats = ref<Record<StatKey, number>>({
  solutionTotal: 0,
  pendingRequests: 0,
  weekDiscussions: 0,
  activeUsers: 0
})
let disposed = false

function animateCount(key: StatKey, to: number, duration = 900) {
  const start = performance.now()
  const step = (now: number) => {
    if (disposed) return
    const progress = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3) // easeOutCubic
    animatedStats.value[key] = Math.round(to * eased)
    if (progress < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

const statCards = computed(() =>
  STAT_KEYS.map((key) => ({ key, label: t('home.stats.' + key), value: animatedStats.value[key] }))
)

const quickActions = computed(() => [
  { key: 'browse', emoji: '🔍', title: t('home.action.browse'), desc: t('home.action.browseDesc'), path: '/opportunity' },
  { key: 'postReq', emoji: '📝', title: t('home.action.postReq'), desc: t('home.action.postReqDesc'), path: '/requirement' },
  { key: 'publish', emoji: '📤', title: t('home.action.publish'), desc: t('home.action.publishDesc'), path: '/opportunity/form' },
  { key: 'discuss', emoji: '💬', title: t('home.action.discuss'), desc: t('home.action.discussDesc'), path: '/discussion' }
])

const pendingCount = computed(() => (dashboard.value?.quickTasks ?? []).filter((x) => x.status === 'pending').length)
const overdueCount = computed(() => (dashboard.value?.quickTasks ?? []).filter((x) => x.isOverdue).length)

// ==== 情报中心面板（引用 apis/intel mock，SSOT）====
interface IntelFeedItem {
  id: string
  badge: string
  title: string
  typeLabel: string
  tone: string
  createdAt: string
}
const intelItems = ref<IntelFeedItem[]>([])

function relativeTime(dateStr: string): string {
  if (!dateStr) return '--'
  const time = new Date(dateStr.replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return dateStr
  const day = Math.floor((Date.now() - time) / 86400000)
  if (day < 1) return t('common.today')
  if (day < 30) return t('common.daysAgo', { n: day })
  return dateStr.slice(0, 10)
}

const intelTone: Record<string, string> = {
  new_product: 'green',
  price_change: 'orange',
  customer_case: 'blue',
  other: 'purple'
}

async function loadIntel() {
  const [comp, ind] = await Promise.all([
    getCompetitorList({ pageNumber: 1, pageSize: 2 }),
    getIndustryList({ pageNumber: 1, pageSize: 1 })
  ])
  const feed: IntelFeedItem[] = comp.records.slice(0, 2).map((c) => ({
    id: c.id,
    badge: c.brand,
    title: c.title,
    typeLabel: t('dict.intelType.' + c.intelType),
    tone: intelTone[c.intelType] || 'blue',
    createdAt: c.createdAt
  }))
  const first = ind.records[0]
  if (first) {
    feed.push({
      id: first.id,
      badge: t('home.intel.industryBadge'),
      title: first.title,
      typeLabel: t('intel.industryDict.' + first.industry),
      tone: 'blue',
      createdAt: first.createdAt
    })
  }
  intelItems.value = feed
}

function onSearch(value: string) {
  const kw = typeof value === 'string' ? value : keyword.value
  router.push({ path: '/opportunity', query: kw ? { keyword: kw } : {} })
}

onMounted(async () => {
  dashboard.value = await getHomeDashboard()
  const stats = dashboard.value.stats
  STAT_KEYS.forEach((key) => animateCount(key, stats[key] ?? 0))
  await loadIntel()
})

onBeforeUnmount(() => {
  disposed = true
})
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, hsl(var(--primary)) 0%, hsl(var(--primary) / 0.7) 100%);
}
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  border-radius: 20px;
  margin-bottom: 12px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}
.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #52c41a;
  position: relative;
}
.live-dot::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: #52c41a;
  animation: pulse-ring 2s ease-out infinite;
}
@keyframes pulse-ring {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(2.2);
    opacity: 0;
  }
}

/* ===== 情报中心面板 ===== */
.intel-panel {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  overflow: hidden;
}
.intel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: linear-gradient(135deg, #0a1628 0%, #102240 50%, #0d1f3c 100%);
}
.intel-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.intel-header-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(19, 194, 194, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #13c2c2;
}
.intel-header-title {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}
.intel-header-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}
.intel-enter {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.intel-enter:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
.intel-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid hsl(var(--line) / 0.6);
}
.intel-item:hover {
  background: hsl(var(--primary) / 0.03);
}
.intel-brand {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  overflow: hidden;
}
.brand-green {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #52c41a;
}
.brand-orange {
  background: linear-gradient(135deg, #fff7e6, #ffd591);
  color: #fa8c16;
}
.brand-blue {
  background: linear-gradient(135deg, #e6f7ff, #bae7ff);
  color: #1890ff;
}
.brand-purple {
  background: linear-gradient(135deg, #f9f0ff, #efdbff);
  color: #722ed1;
}
.intel-main {
  flex: 1;
  min-width: 0;
}
.intel-title {
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}
.intel-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.intel-type {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
}
.type-green {
  background: rgba(82, 196, 26, 0.12);
  color: #52c41a;
}
.type-orange {
  background: rgba(250, 140, 22, 0.12);
  color: #fa8c16;
}
.type-blue {
  background: rgba(24, 144, 255, 0.12);
  color: #1890ff;
}
.type-purple {
  background: rgba(114, 46, 209, 0.12);
  color: #722ed1;
}
.intel-time {
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
.intel-arrow {
  color: hsl(var(--line));
  flex-shrink: 0;
}
.intel-empty {
  padding: 20px 0;
}
.intel-links {
  display: flex;
  border-top: 1px solid hsl(var(--line));
}
.intel-link {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 13px 0;
  font-size: 12px;
  color: hsl(var(--secondary-text));
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border-right: 1px solid hsl(var(--line));
}
.intel-link:last-child {
  border-right: none;
}
.intel-link:hover {
  background: hsl(var(--card-bg));
  color: hsl(var(--primary));
}

.stat-card {
  background: hsl(var(--card-bg));
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px hsl(var(--primary) / 0.12);
}

.sol-item {
  background: hsl(var(--card-bg));
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}
.sol-item:hover {
  background: hsl(var(--primary) / 0.06);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px hsl(var(--primary) / 0.12);
}

/* 公告 / 讨论热帖 列表项可点击悬浮反馈 */
.list-link {
  transition: background 0.2s ease;
}
.list-link:hover {
  background: hsl(var(--primary) / 0.06);
}

/* 逾期任务红色脉冲指示 */
.overdue-dot {
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  display: inline-block;
  background: hsl(var(--error));
}
.pulse {
  animation: pulse 1.1s ease-in-out infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.35;
  }
}

.rank {
  color: hsl(var(--primary));
  font-weight: 700;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  display: inline-block;
  flex-shrink: 0;
}
.dot-critical {
  background: hsl(var(--error));
}
.dot-urgent {
  background: #fa8c16;
}
.dot-normal {
  background: hsl(var(--secondary-text));
}
</style>
