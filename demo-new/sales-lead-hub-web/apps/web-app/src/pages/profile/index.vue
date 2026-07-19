<template>
  <div class="profile-page">
    <!-- 用户卡 -->
    <div class="user-card">
      <a-avatar :size="72" class="profile-avatar">{{ avatarText }}</a-avatar>
      <div class="user-main">
        <div class="user-name-row">
          <span class="user-name">{{ data.user.name || '--' }}</span>
          <a-tag color="blue">{{ data.user.roleName || '--' }}</a-tag>
        </div>
        <div class="user-meta">
          <span><UserOutlined /> {{ data.user.deptName || '--' }}</span>
          <span>{{ t('profile.employeeNo') }}：{{ data.user.employeeNo || '--' }}</span>
        </div>
        <div class="sso-hint">
          <InfoCircleOutlined /> {{ t('profile.ssoManaged') }}
        </div>
      </div>
      <div class="stat-cards">
        <StatCard :label="t('profile.stats.collect')" :value="favCount" accent="#1890ff" clickable
          @click="goTab('collect')">
          <template #icon><StarOutlined /></template>
        </StatCard>
        <StatCard :label="t('profile.stats.comment')" :value="activeCommentCount" accent="#13c2c2" clickable
          @click="goTab('comment')">
          <template #icon><MessageOutlined /></template>
        </StatCard>
        <StatCard :label="t('profile.stats.publish')" :value="publishedTotal" accent="#52c41a" clickable
          @click="goTab('publish')">
          <template #icon><FileTextOutlined /></template>
        </StatCard>
        <StatCard :label="t('profile.stats.solution')" :value="solutionCount" accent="#fa8c16" clickable
          @click="goTab('solution')">
          <template #icon><TrophyOutlined /></template>
        </StatCard>
        <StatCard :label="t('profile.stats.draft')" :value="draftCount" accent="#faad14" clickable
          @click="goTab('publish', 'draft')">
          <template #icon><EditOutlined /></template>
        </StatCard>
        <StatCard :label="t('profile.stats.view')" :value="historyCount" accent="#722ed1" clickable
          @click="goTab('history')">
          <template #icon><ClockCircleOutlined /></template>
        </StatCard>
      </div>
    </div>

    <!-- Tabs -->
    <a-tabs v-model:activeKey="activeTab" class="profile-tabs">
      <!-- 订阅设置 -->
      <a-tab-pane key="subscription" :tab="t('profile.tab.subscription')">
        <div class="pane">
          <a-alert type="info" show-icon class="mb-3" :message="t('profile.defaultPushTip')" />

          <div class="section-row">
            <span class="section-label">{{ t('profile.categorySource') }}</span>
            <a-segmented :value="subSource" :options="sourceOptions" @update:value="(v: any) => (subSource = v)" />
          </div>

          <div class="section-label mt-4">{{ t('profile.currentSubscription') }}</div>
          <div class="tag-row">
            <template v-if="activeSubs.length">
              <a-tag v-for="v in activeSubs" :key="v" closable
                :color="subSource === 'opportunity' ? 'blue' : 'orange'" @close="removeTag(v)">
                {{ labelMap[v] || v }}
              </a-tag>
            </template>
            <span v-else class="sub-empty"><InfoCircleOutlined /> {{ t('profile.subEmptyHint') }}</span>
          </div>

          <div class="section-label mt-4">{{ t('profile.selectCategory') }}</div>
          <div class="preset-row">
            <template v-if="subSource === 'opportunity'">
              <a-button size="small" @click="applyPreset('module')">{{ t('profile.presetAllModule') }}</a-button>
              <a-button size="small" @click="applyPreset('antenna')">{{ t('profile.presetAllAntenna') }}</a-button>
            </template>
            <template v-else>
              <a-button size="small" @click="applyPreset('type')">{{ t('profile.presetAllType') }}</a-button>
              <a-button size="small" @click="applyPreset('field')">{{ t('profile.presetAllField') }}</a-button>
            </template>
            <a-button size="small" @click="applyPreset('clear')">{{ t('profile.presetClear') }}</a-button>
          </div>

          <a-tree-select
            :key="subSource"
            :value="activeSubs"
            class="sub-tree"
            @update:value="(v: any) => (activeSubs = v)"
            :tree-data="currentTree"
            tree-checkable
            :show-checked-strategy="showChild"
            :placeholder="t('profile.subscribePlaceholder')"
            tree-default-expand-all
            allow-clear
            max-tag-count="responsive"
          />

          <div class="sub-summary">
            {{ t('profile.subSummary', { opp: oppSubs.length, req: reqSubs.length, total: totalSubCount }) }}
          </div>

          <a-button type="primary" class="mt-4" @click="saveSubscription">
            <template #icon><SaveOutlined /></template>
            {{ t('common.save') }}
          </a-button>

          <!-- 通知偏好入口 -->
          <div class="notify-pref">
            <div class="notify-pref-title"><BellOutlined /> {{ t('profile.notifyPref') }}</div>
            <div class="notify-pref-body">
              <span class="notify-pref-desc">{{ t('profile.notifyPrefDesc') }}</span>
              <a-button type="link" @click="goNotifyPreference">
                <template #icon><SettingOutlined /></template>
                {{ t('profile.notifyPrefEntry') }}
              </a-button>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- 我的收藏 -->
      <a-tab-pane key="collect" :tab="t('profile.tab.collect')">
        <div class="pane">
          <div class="section-row">
            <a-segmented :value="favSeg" :options="favSegOptions" @update:value="(v: any) => (favSeg = v)" />
            <span class="muted">{{ t('profile.collectTotal', { n: favCount }) }}</span>
          </div>
          <Empty v-if="!filteredCollects.length" type="noData" />
          <div v-for="item in filteredCollects" :key="item.id" class="row-item" :class="{ 'is-deleted': item.isDeleted }">
            <div class="row-main">
              <span class="link-title" @click="!item.isDeleted && goDetail(item.type, item.id)">{{ item.title }}</span>
              <a-tag :color="item.type === 'opportunity' ? 'blue' : 'orange'">{{ t('profile.itemType.' + item.type) }}</a-tag>
              <a-tag v-if="item.isDeleted" color="red">{{ t('profile.deleted') }}</a-tag>
              <span class="muted-sm">{{ item.createdAt }}</span>
            </div>
            <a-button v-if="!item.isDeleted" type="text" danger size="small" @click="cancelCollect(item.id)">
              {{ t('profile.cancelCollect') }}
            </a-button>
          </div>
        </div>
      </a-tab-pane>

      <!-- 我的发布 -->
      <a-tab-pane key="publish" :tab="t('profile.tab.publish')">
        <div class="pane">
          <div class="pub-stat-row">
            <div class="pub-stat" :class="{ active: pubFilter === 'draft' }" @click="pubFilter = 'draft'">
              <div class="ps-num">{{ draftCount }}</div>
              <div class="ps-label">{{ t('dict.oppStatus.draft') }}</div>
            </div>
            <div class="pub-stat" :class="{ active: pubFilter === 'published' }" @click="pubFilter = 'published'">
              <div class="ps-num">{{ publishedCount }}</div>
              <div class="ps-label">{{ t('dict.oppStatus.published') }}</div>
            </div>
            <div class="pub-stat" :class="{ active: pubFilter === 'archived' }" @click="pubFilter = 'archived'">
              <div class="ps-num">{{ archivedCount }}</div>
              <div class="ps-label">{{ t('dict.oppStatus.archived') }}</div>
            </div>
            <div class="pub-stat" :class="{ active: pubFilter === 'all' }" @click="pubFilter = 'all'">
              <div class="ps-num">{{ data.publishes.length }}</div>
              <div class="ps-label">{{ t('common.all') }}</div>
            </div>
          </div>

          <!-- 草稿专区 -->
          <div v-if="draftZoneVisible" class="draft-zone">
            <div class="draft-zone-title"><EditOutlined /> {{ t('profile.draftZone', { n: draftCount }) }}</div>
            <div v-for="item in drafts" :key="item.id" class="row-item draft-item">
              <div class="row-main col">
                <div class="draft-head">
                  <a-tag>{{ t('dict.oppStatus.draft') }}</a-tag>
                  <span class="link-title" @click="goPublishEdit(item.id)">{{ item.title }}</span>
                </div>
                <div class="muted-sm">
                  <EditOutlined /> {{ t('profile.lastEdited', { time: item.editedAt }) }}
                  <a-tag :color="item.type === 'opportunity' ? 'blue' : 'orange'">{{ t('profile.itemType.' + item.type) }}</a-tag>
                </div>
              </div>
              <div class="row-actions">
                <a-button type="primary" size="small" @click="goPublishEdit(item.id)">
                  <template #icon><EditOutlined /></template>
                  {{ t('profile.continueEdit') }}
                </a-button>
                <a-popconfirm :title="t('profile.deleteDraftConfirm')" :ok-text="t('common.confirm')"
                  :cancel-text="t('common.cancel')" @confirm="deletePublish(item.id)">
                  <a-button type="text" danger size="small"><template #icon><DeleteOutlined /></template></a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>

          <a-segmented :value="pubFilter" :options="pubStatusOptions" class="mb-3" @update:value="(v: any) => (pubFilter = v)" />

          <Empty v-if="!filteredPublished.length" type="noData" />
          <div v-for="item in filteredPublished" :key="item.id" class="row-item col pub-record">
            <div class="pub-head">
              <span class="link-title" @click="goDetail(item.type, item.id)">{{ item.title }}</span>
              <a-tag :color="item.type === 'opportunity' ? 'blue' : 'orange'">{{ t('profile.itemType.' + item.type) }}</a-tag>
              <a-tag :color="statusColor[item.status] || 'default'">{{ t('dict.oppStatus.' + item.status) }}</a-tag>
              <a-tag v-if="item.isAdopted" color="green"><StarFilled /> {{ t('profile.adoptedStar') }}</a-tag>
              <span class="muted-sm">{{ item.createdAt }}</span>
            </div>
            <!-- 浏览量数据条 -->
            <div class="analytics-bar">
              <div class="fill" :style="{ width: barWidth(item.viewCount) + '%' }"></div>
            </div>
            <div class="data-line">
              <span><EyeOutlined /> {{ item.viewCount }} {{ t('common.viewCount') }}</span>
              <span><LikeOutlined /> {{ item.likeCount }} {{ t('common.like') }}</span>
              <span><MessageOutlined /> {{ item.commentCount }} {{ t('common.comment') }}</span>
              <span><StarOutlined /> {{ item.collectCount }} {{ t('common.collect') }}</span>
            </div>
            <!-- 收到的回复 -->
            <div v-if="item.replies.length" class="reply-block">
              <div class="reply-head"><CommentOutlined /> {{ t('profile.replyCount', { n: item.replies.length }) }}</div>
              <div v-for="r in item.replies" :key="r.id" class="reply-item">
                <div class="reply-body">
                  <div class="reply-content">{{ r.content }}</div>
                  <div class="muted-sm"><UserOutlined /> {{ r.fromName }} · <ClockCircleOutlined /> {{ r.repliedAt }}</div>
                </div>
                <a-popconfirm :title="t('profile.deleteReplyConfirm')" :ok-text="t('common.confirm')"
                  :cancel-text="t('common.cancel')" @confirm="deleteReply(item.id, r.id)">
                  <a-button type="text" danger size="small"><template #icon><DeleteOutlined /></template></a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- 我的方案 -->
      <a-tab-pane key="solution" :tab="t('profile.tab.solution')">
        <div class="pane">
          <div class="muted mb-3">
            {{ t('profile.solutionSummary', { n: solutionCount }) }} · {{ t('profile.adoptNotify') }}
          </div>
          <Empty v-if="!data.solutions.length" type="noData" />
          <div v-for="item in data.solutions" :key="item.id" class="solution-card">
            <div class="sol-head">
              <TrophyOutlined class="trophy" />
              <span class="sol-best">{{ t('profile.bestSolution') }}</span>
              <a-tag color="orange">{{ t('profile.adopted') }}</a-tag>
              <span class="muted-sm ml-auto">{{ item.adoptedAt }}</span>
            </div>
            <div class="sol-req">
              <span class="muted">{{ t('profile.reqLabel') }}：</span>
              <span class="link-title" @click="goDetail('requirement', item.requestId)">{{ item.requestTitle }}</span>
            </div>
            <div class="sol-summary">
              <span class="muted-sm">{{ t('profile.summaryLabel') }}：</span>{{ truncate(item.summary, 100) }}
            </div>
            <div class="sol-foot">
              <span class="muted-sm">{{ t('profile.adoptedByLine', { name: item.adopterName, dept: item.adopterDeptName }) }}</span>
              <a-button type="link" size="small" @click="goDetail('requirement', item.requestId)">
                {{ t('profile.viewRequest') }}<template #icon><ArrowRightOutlined /></template>
              </a-button>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- 我的评论 -->
      <a-tab-pane key="comment" :tab="t('profile.tab.comment')">
        <div class="pane">
          <div class="muted mb-3">
            {{ t('profile.commentSummary', { n: activeCommentCount }) }}
            {{ t('profile.includeDeleted', { n: deletedCommentCount }) }}
          </div>
          <Empty v-if="!data.comments.length" type="noData" />
          <div class="timeline">
            <div v-for="item in data.comments" :key="item.id" class="tl-item" :class="{ deleted: item.isDeleted }">
              <div class="comment-card" :class="{ 'is-deleted': item.isDeleted }">
                <div class="cmt-content">
                  <span v-if="item.isDeleted" class="deleted-text">{{ t('profile.deletedComment') }}</span>
                  <span v-else>
                    {{ isExpanded(item.id) || item.content.length <= 120 ? item.content : truncate(item.content, 120) }}
                    <span v-if="item.content.length > 120" class="expand-link" @click="toggleExpand(item.id)">
                      {{ isExpanded(item.id) ? t('profile.collapse') : t('profile.expand') }}
                    </span>
                  </span>
                </div>
                <div class="cmt-meta">
                  <span class="cmt-source" @click="!item.isDeleted && goDetail(item.sourceType, item.sourceId)">
                    {{ t('profile.commentedOn') }}
                    <a-tag :color="item.sourceType === 'opportunity' ? 'blue' : 'orange'">{{ t('profile.itemType.' + item.sourceType) }}</a-tag>
                    <span class="cmt-source-title">{{ item.sourceTitle }}</span>
                  </span>
                  <span class="muted-sm">{{ item.createdAt }}</span>
                  <a-button v-if="!item.isDeleted" type="link" danger size="small" class="p-0" @click="deleteComment(item.id)">
                    {{ t('common.delete') }}
                  </a-button>
                  <a-button v-if="!item.isDeleted" type="link" size="small" class="p-0" @click="goDetail(item.sourceType, item.sourceId)">
                    {{ t('profile.viewOrigin') }}
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- 浏览记录 -->
      <a-tab-pane key="history" :tab="t('profile.tab.history')">
        <div class="pane">
          <div class="section-row">
            <span class="muted">{{ t('profile.historySummary', { n: historyCount }) }}</span>
            <a-popconfirm v-if="historyCount" :title="t('profile.clearHistoryConfirm')" :ok-text="t('common.confirm')"
              :cancel-text="t('common.cancel')" @confirm="clearHistory">
              <a-button size="small" danger><template #icon><DeleteOutlined /></template>{{ t('profile.clearAll') }}</a-button>
            </a-popconfirm>
          </div>
          <Empty v-if="!data.viewHistory.length" type="noData" />
          <div v-for="item in data.viewHistory" :key="item.id" class="history-item">
            <div class="hi-main">
              <ClockCircleOutlined class="hi-clock" />
              <span class="link-title" @click="goDetail(item.type, item.id)">{{ item.title }}</span>
              <a-tag :color="item.type === 'opportunity' ? 'blue' : 'orange'">{{ t('profile.itemType.' + item.type) }}</a-tag>
            </div>
            <span class="muted-sm">{{ item.viewedAt }}</span>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal, TreeSelect } from 'ant-design-vue'
import {
  UserOutlined, InfoCircleOutlined, StarOutlined, StarFilled, MessageOutlined,
  FileTextOutlined, TrophyOutlined, EditOutlined, ClockCircleOutlined,
  SaveOutlined, BellOutlined, SettingOutlined, DeleteOutlined, EyeOutlined,
  LikeOutlined, CommentOutlined, ArrowRightOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import StatCard from '@/components/stat-card/index.vue'
import { getProfile } from '@/apis/profile/profileApi'
import type { ProfileAggregate, SubscriptionNode } from '@/apis/profile/types'

defineOptions({ name: 'ProfileCenter' })
definePage({
  name: 'ProfileCenter',
  meta: {
    layout: 'default',
    order: 4,
    menu: { icon: 'q-icon:bookmark-linear' },
    title: 'profile'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const showChild = TreeSelect.SHOW_CHILD
const statusColor: Record<string, string> = { published: 'green', archived: 'red', draft: 'default' }
const TAB_KEYS = ['subscription', 'collect', 'publish', 'solution', 'comment', 'history']

const data = reactive<ProfileAggregate>({
  user: { name: '', deptName: '', roleName: '', employeeNo: '' },
  stats: { collectCount: 0, commentCount: 0, publishCount: 0, solutionCount: 0, draftCount: 0, viewCount: 0 },
  subscriptionTree: { opportunity: [], requirement: [] },
  subscribedKeys: { opportunity: [], requirement: [] },
  collects: [],
  publishes: [],
  solutions: [],
  comments: [],
  viewHistory: []
})

// ---- Tab / URL hash 双向同步 ----
function readHash(): string {
  const h = (window.location.hash || '').replace('#', '')
  return TAB_KEYS.includes(h) ? h : 'subscription'
}
const activeTab = ref(readHash())
function onHashChange() { activeTab.value = readHash() }
watch(activeTab, (v) => {
  if (('#' + v) !== window.location.hash) window.location.hash = v
})

// ---- 订阅设置 ----
const subSource = ref<'opportunity' | 'requirement'>('opportunity')
const oppSubs = ref<string[]>([])
const reqSubs = ref<string[]>([])
const pubFilter = ref<'all' | 'draft' | 'published' | 'archived'>('all')
const favSeg = ref<'opportunity' | 'requirement'>('opportunity')
const expandedIds = ref<string[]>([])

const sourceOptions = computed(() => [
  { label: t('profile.sourceOpp'), value: 'opportunity' },
  { label: t('profile.sourceReq'), value: 'requirement' }
])
const favSegOptions = computed(() => [
  { label: t('profile.itemType.opportunity'), value: 'opportunity' },
  { label: t('profile.itemType.requirement'), value: 'requirement' }
])
const pubStatusOptions = computed(() => [
  { label: t('common.all'), value: 'all' },
  { label: t('dict.oppStatus.published'), value: 'published' },
  { label: t('dict.oppStatus.archived'), value: 'archived' }
])

const currentTree = computed<SubscriptionNode[]>(() =>
  subSource.value === 'opportunity' ? data.subscriptionTree.opportunity : data.subscriptionTree.requirement
)
const activeSubs = computed<string[]>({
  get: () => (subSource.value === 'opportunity' ? oppSubs.value : reqSubs.value),
  set: (v) => { if (subSource.value === 'opportunity') oppSubs.value = v; else reqSubs.value = v }
})
const totalSubCount = computed(() => oppSubs.value.length + reqSubs.value.length)

// value → 分类名称 映射（用于可关闭 Tag）
const labelMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  const walk = (nodes: SubscriptionNode[]) => {
    nodes.forEach((n) => {
      map[n.value] = n.title
      if (n.children) walk(n.children)
    })
  }
  walk(data.subscriptionTree.opportunity)
  walk(data.subscriptionTree.requirement)
  return map
})

function leavesOf(nodes: SubscriptionNode[], topKey: string): string[] {
  const top = nodes.find((n) => n.key === topKey)
  if (!top || !top.children) return []
  return top.children.map((c) => c.value)
}
function applyPreset(kind: string) {
  if (subSource.value === 'opportunity') {
    if (kind === 'module') oppSubs.value = leavesOf(data.subscriptionTree.opportunity, 'iot-module')
    else if (kind === 'antenna') oppSubs.value = leavesOf(data.subscriptionTree.opportunity, 'antenna')
    else if (kind === 'clear') oppSubs.value = []
  } else {
    if (kind === 'type') reqSubs.value = leavesOf(data.subscriptionTree.requirement, 'req-type')
    else if (kind === 'field') reqSubs.value = leavesOf(data.subscriptionTree.requirement, 'app-field')
    else if (kind === 'clear') reqSubs.value = []
  }
}
function removeTag(v: string) {
  activeSubs.value = activeSubs.value.filter((x) => x !== v)
}
function saveSubscription() {
  if (totalSubCount.value === 0) message.success(t('profile.saveSubEmpty'))
  else message.success(t('profile.saveSubOk', { total: totalSubCount.value }))
}
function goNotifyPreference() {
  router.push({ path: '/notification/preference' })
}

// ---- 统计（自列表派生，SSOT）----
const favCount = computed(() => data.collects.filter((c) => !c.isDeleted).length)
const activeCommentCount = computed(() => data.comments.filter((c) => !c.isDeleted).length)
const deletedCommentCount = computed(() => data.comments.filter((c) => c.isDeleted).length)
const draftCount = computed(() => data.publishes.filter((p) => p.status === 'draft').length)
const publishedCount = computed(() => data.publishes.filter((p) => p.status === 'published').length)
const archivedCount = computed(() => data.publishes.filter((p) => p.status === 'archived').length)
const publishedTotal = computed(() => data.publishes.filter((p) => p.status !== 'draft').length)
const solutionCount = computed(() => data.solutions.length)
const historyCount = computed(() => data.viewHistory.length)

const avatarText = computed(() => (data.user.name ? data.user.name.slice(0, 1) : '--'))

// ---- 收藏 ----
const filteredCollects = computed(() => data.collects.filter((c) => c.type === favSeg.value))
function cancelCollect(id: string) {
  const idx = data.collects.findIndex((c) => c.id === id)
  if (idx >= 0) data.collects.splice(idx, 1)
  message.success(t('common.success'))
}

// ---- 发布 ----
const drafts = computed(() => data.publishes.filter((p) => p.status === 'draft'))
const draftZoneVisible = computed(() => (pubFilter.value === 'all' || pubFilter.value === 'draft') && drafts.value.length > 0)
const filteredPublished = computed(() => {
  const base = pubFilter.value === 'all' ? data.publishes : data.publishes.filter((p) => p.status === pubFilter.value)
  return base.filter((p) => p.status !== 'draft')
})
function barWidth(views: number): number {
  return Math.min(100, Math.round((views / 350) * 100))
}
function goPublishEdit(id: string) {
  router.push({ path: '/opportunity/form', query: { id } })
}
function deletePublish(id: string) {
  const idx = data.publishes.findIndex((p) => p.id === id)
  if (idx >= 0) data.publishes.splice(idx, 1)
  message.success(t('common.success'))
}
function deleteReply(pubId: string, replyId: string) {
  const p = data.publishes.find((x) => x.id === pubId)
  if (p) p.replies = p.replies.filter((r) => r.id !== replyId)
  message.success(t('common.success'))
}

// ---- 评论 ----
function isExpanded(id: string): boolean {
  return expandedIds.value.includes(id)
}
function toggleExpand(id: string) {
  if (isExpanded(id)) expandedIds.value = expandedIds.value.filter((x) => x !== id)
  else expandedIds.value = [...expandedIds.value, id]
}
function deleteComment(id: string) {
  Modal.confirm({
    title: t('profile.deleteCommentConfirm'),
    okText: t('common.confirm'),
    okType: 'danger',
    cancelText: t('common.cancel'),
    onOk: () => {
      const c = data.comments.find((x) => x.id === id)
      if (c) c.isDeleted = true
      message.success(t('common.success'))
    }
  })
}

// ---- 浏览记录 ----
function clearHistory() {
  data.viewHistory = []
  message.success(t('common.success'))
}

// ---- 通用 ----
function truncate(text: string, max: number): string {
  return text.length > max ? text.slice(0, max) + '...' : text
}
function goTab(key: string, publishSub?: string) {
  activeTab.value = key
  if (key === 'publish' && publishSub === 'draft') pubFilter.value = 'draft'
}
function goDetail(type: string, id: string) {
  const path = type === 'requirement' ? '/requirement/detail' : '/opportunity/detail'
  router.push({ path, query: { id } })
}

onMounted(async () => {
  window.addEventListener('hashchange', onHashChange)
  const res = await getProfile()
  Object.assign(data, res)
  oppSubs.value = [...data.subscribedKeys.opportunity]
  reqSubs.value = [...data.subscribedKeys.requirement]
})
onUnmounted(() => {
  window.removeEventListener('hashchange', onHashChange)
})
</script>

<style scoped>
.profile-page {
  max-width: 1040px;
  margin: 0 auto;
  padding: 16px;
}
.mb-3 { margin-bottom: 12px; }
.mt-4 { margin-top: 16px; }
.ml-auto { margin-left: auto; }
.p-0 { padding: 0; }
.muted { font-size: 13px; color: hsl(var(--secondary-text)); }
.muted-sm { font-size: 12px; color: hsl(var(--secondary-text)); }

/* 用户卡 */
.user-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 14px;
  padding: 24px 28px;
  display: flex;
  align-items: flex-start;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.profile-avatar {
  background: linear-gradient(135deg, hsl(var(--primary)), #722ed1) !important;
  font-size: 28px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-main { flex: 1; min-width: 200px; }
.user-name-row { display: flex; align-items: center; gap: 10px; }
.user-name { font-size: 20px; font-weight: 700; color: hsl(var(--text)); }
.user-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 8px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.sso-hint {
  margin-top: 8px;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 1fr));
  gap: 12px;
  flex: 1 1 480px;
}

/* Tabs 容器 */
.profile-tabs {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 14px;
  padding: 4px 16px 16px;
}
.pane { padding-top: 8px; }
.section-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.section-label { font-weight: 600; font-size: 14px; color: hsl(var(--text)); display: block; margin-bottom: 8px; }
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 32px;
}
.sub-empty { font-size: 13px; color: hsl(var(--secondary-text)); }
.preset-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.sub-tree { width: 100%; max-width: 620px; display: block; }
.sub-summary { margin-top: 10px; font-size: 13px; color: hsl(var(--secondary-text)); }

/* 通知偏好入口 */
.notify-pref {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid hsl(var(--line));
}
.notify-pref-title {
  font-weight: 600;
  font-size: 14px;
  color: hsl(var(--text));
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.notify-pref-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.notify-pref-desc { font-size: 13px; color: hsl(var(--secondary-text)); }

/* 通用行 */
.row-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  margin-bottom: 8px;
  transition: box-shadow 0.2s;
}
.row-item:hover { box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06); }
.row-item.col { flex-direction: column; align-items: stretch; }
.row-main { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; flex: 1; min-width: 0; }
.row-main.col { flex-direction: column; align-items: flex-start; gap: 6px; }
.row-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.link-title { color: hsl(var(--primary)); font-weight: 500; cursor: pointer; }
.link-title:hover { text-decoration: underline; }
.is-deleted { opacity: 0.5; }
.is-deleted .link-title { color: hsl(var(--secondary-text)); pointer-events: none; }

/* 发布统计卡 */
.pub-stat-row { display: flex; gap: 12px; margin-bottom: 18px; flex-wrap: wrap; }
.pub-stat {
  flex: 1 1 120px;
  text-align: center;
  padding: 14px 12px;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.pub-stat:hover { border-color: hsl(var(--primary) / 0.4); transform: translateY(-2px); }
.pub-stat.active { border-color: hsl(var(--primary)); background: hsl(var(--primary) / 0.06); }
.ps-num { font-size: 22px; font-weight: 700; color: hsl(var(--text)); }
.pub-stat.active .ps-num { color: hsl(var(--primary)); }
.ps-label { font-size: 12px; color: hsl(var(--secondary-text)); }

/* 草稿专区 */
.draft-zone { margin-bottom: 20px; }
.draft-zone-title {
  font-weight: 600;
  font-size: 14px;
  color: #d48806;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}
.draft-item { background: hsl(45deg 100% 96%); border-left: 3px solid #faad14; }
.draft-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* 发布记录 */
.pub-record { gap: 8px; }
.pub-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.analytics-bar {
  height: 6px;
  border-radius: 3px;
  background: hsl(var(--card-bg));
  overflow: hidden;
}
.analytics-bar .fill {
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(90deg, hsl(var(--primary)), #36cfc9);
  transition: width 0.6s ease;
}
.data-line { display: flex; gap: 16px; flex-wrap: wrap; font-size: 12px; color: hsl(var(--secondary-text)); }
.reply-block { margin-top: 8px; padding-top: 10px; border-top: 1px solid hsl(var(--line)); }
.reply-head { font-size: 12px; color: hsl(var(--secondary-text)); margin-bottom: 8px; }
.reply-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  background: hsl(var(--card-bg));
  border-radius: 8px;
  margin-bottom: 6px;
}
.reply-body { flex: 1; min-width: 0; }
.reply-content { font-size: 13px; color: hsl(var(--text)); line-height: 1.6; word-break: break-word; }

/* 我的方案 */
.solution-card {
  border: 1px solid hsl(var(--line));
  border-left: 3px solid #fa8c16;
  background: hsl(45deg 100% 96%);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 10px;
}
.sol-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
.trophy { color: #fa8c16; font-size: 18px; }
.sol-best { font-weight: 600; font-size: 14px; color: hsl(var(--text)); }
.sol-req { margin-bottom: 6px; font-size: 13px; }
.sol-summary {
  font-size: 13px;
  color: hsl(var(--text));
  line-height: 1.6;
  background: #fff;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid hsl(var(--line));
  margin-bottom: 8px;
}
.sol-foot { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; }

/* 我的评论时间线 */
.timeline { position: relative; padding-left: 8px; }
.tl-item { position: relative; padding-left: 24px; padding-bottom: 20px; }
.tl-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: hsl(var(--primary));
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px hsl(var(--primary));
  z-index: 1;
}
.tl-item::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 20px;
  bottom: 0;
  width: 2px;
  background: hsl(var(--line));
}
.tl-item:last-child::after { display: none; }
.tl-item.deleted::before { background: hsl(var(--line)); box-shadow: 0 0 0 2px hsl(var(--line)); }
.comment-card {
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  padding: 14px 16px;
  transition: box-shadow 0.2s;
}
.comment-card:hover { box-shadow: 0 4px 14px rgba(0, 0, 0, 0.05); }
.comment-card.is-deleted { opacity: 0.6; }
.cmt-content { font-size: 14px; color: hsl(var(--text)); line-height: 1.7; margin-bottom: 10px; }
.deleted-text { color: hsl(var(--error)); font-style: italic; }
.expand-link { color: hsl(var(--primary)); cursor: pointer; margin-left: 4px; white-space: nowrap; }
.cmt-meta { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; font-size: 12px; color: hsl(var(--secondary-text)); }
.cmt-source { color: hsl(var(--primary)); cursor: pointer; }
.cmt-source:hover { text-decoration: underline; }
.cmt-source-title { color: hsl(var(--text)); }

/* 浏览记录 */
.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  margin-bottom: 8px;
  transition: box-shadow 0.2s;
}
.history-item:hover { box-shadow: 0 4px 14px rgba(0, 0, 0, 0.05); }
.hi-main { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.hi-clock { color: hsl(var(--secondary-text)); font-size: 16px; }

@media (max-width: 900px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); flex-basis: 100%; }
}
@media (max-width: 600px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .row-item { flex-direction: column; align-items: flex-start; }
}
</style>
