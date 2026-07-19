<template>
  <div v-if="!detail" class="h-full flex items-center justify-center bg-white rounded">
    <Empty type="noData" />
  </div>
  <div v-else class="page-wrap">
    <div class="nav-back" @click="goBack"><ArrowLeftOutlined /> {{ t('intel.backToCenter') }}</div>

    <!-- Hero -->
    <div class="hero-card ind">
      <div class="hero-tags">
        <a-tag color="blue"><component :is="industryIcon[detail.industry] || GlobalOutlined" /> {{ t('intel.industryDict.' + detail.industry) }}</a-tag>
      </div>
      <h1 class="hero-title">{{ detail.title }}</h1>
      <div class="hero-meta">
        <span><LinkOutlined /> {{ t('intel.source') }}：{{ detail.source }}</span>
        <span class="divider" />
        <span><CalendarOutlined /> {{ (detail.createdAt || '').slice(0, 10) }}</span>
      </div>
    </div>

    <!-- 正文 -->
    <div class="content-card">
      <div class="summary-highlight ind">
        <strong>{{ t('intel.summaryLabelIndustry') }}</strong>{{ detail.summary }}
      </div>

      <h2>{{ t('intel.overviewIndustry') }}</h2>
      <p>{{ detail.overview }}</p>

      <h3>{{ t('intel.keyPoints') }}</h3>
      <ul class="key-points">
        <li v-for="(kp, i) in detail.keyPoints" :key="i">{{ kp }}</li>
      </ul>

      <h2>{{ t('intel.analysisIndustry') }}</h2>
      <p>{{ detail.analysis }}</p>

      <h2>{{ t('intel.impactIndustry') }}</h2>
      <p>{{ detail.impact }}</p>

      <!-- 行业类型专属应对建议 -->
      <div v-if="adviceItems.length" class="highlight-box">
        <strong>{{ adviceTitle }}</strong>
        <ul>
          <li v-for="(line, i) in adviceItems" :key="i">{{ line }}</li>
        </ul>
      </div>

      <div class="source-ref">
        <LinkOutlined />
        <div>
          <div class="src-title">{{ t('intel.sourceInfo') }}</div>
          <div>{{ detail.source }} · {{ t('intel.publishedOn') }} {{ (detail.createdAt || '').slice(0, 10) }}</div>
        </div>
      </div>
    </div>

    <!-- 互动栏（行业页无分享） -->
    <div class="interact-card">
      <SocialStats
        :views="viewCount" :likes="likeCount" :collects="collectCount"
        :liked="liked" :collected="collected" interactive
        @like="onLike" @collect="onCollect"
      />
    </div>

    <!-- 评论区 -->
    <div class="content-card">
      <div class="section-title"><MessageOutlined /> {{ t('comment.section') }} ({{ commentCount }})</div>
      <CommentThread :comments="comments" :loading="commentsLoading"
        @submit="onCommentSubmit" @like="onCommentLike" />
    </div>

    <!-- 相关推荐 -->
    <div v-if="related.length" class="content-card">
      <div class="section-title"><RiseOutlined /> {{ t('intel.relatedIndustry') }}</div>
      <div class="related-grid">
        <div v-for="r in related" :key="r.id" class="related-item" @click="goIndustry(r.id)">
          <component :is="industryIcon[r.industry] || GlobalOutlined" class="related-icon" />
          <div class="related-main">
            <div class="related-title">{{ r.title }}</div>
            <div class="related-sub">{{ r.source }} · {{ (r.createdAt || '').slice(0, 10) }}</div>
          </div>
          <RightOutlined class="related-arrow" />
        </div>
      </div>
    </div>

    <div class="page-footer">
      <div>{{ t('intel.footerProvided') }}</div>
      <div>{{ t('intel.footerDisclaimer') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import type { Component } from 'vue'
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined, LinkOutlined, CalendarOutlined, MessageOutlined, RightOutlined,
  GlobalOutlined, RiseOutlined, CarOutlined, SafetyCertificateOutlined,
  ThunderboltOutlined, BuildOutlined, ClusterOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import SocialStats from '@/components/social-stats/index.vue'
import CommentThread from '@/components/comment-thread/index.vue'
import { getIndustryDetail, getIndustryList } from '@/apis/intel/intelApi'
import { getComments, addComment, likeComment } from '@/apis/interaction/interactionApi'
import type { IndustryIntelItem } from '@/apis/intel/types'
import type { Comment } from '@/apis/interaction/types'

defineOptions({ name: 'IntelIndustryDetail' })
definePage({
  name: 'IntelIndustryDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'intel.industry'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<IndustryIntelItem | null>(null)
const related = ref<IndustryIntelItem[]>([])

const industryIcon: Record<string, Component> = {
  trend: RiseOutlined,
  automotive: CarOutlined,
  policy: SafetyCertificateOutlined,
  energy: ThunderboltOutlined,
  industrial: BuildOutlined,
  smartcity: ClusterOutlined
}

// 行业类型专属应对建议：政策/趋势/垂直行业
const adviceKey = computed(() => {
  if (!detail.value) return ''
  const ind = detail.value.industry
  if (ind === 'policy') return 'advicePolicy'
  if (ind === 'trend') return 'adviceTrend'
  return 'adviceVertical'
})
const adviceTitle = computed(() => (adviceKey.value ? t('intel.' + adviceKey.value + 'Title') : ''))
const adviceItems = computed(() =>
  adviceKey.value ? t('intel.' + adviceKey.value).split('\n').filter((l) => l.trim()) : []
)

// 互动
const TARGET_TYPE = 'IndustryIntel'
const viewCount = ref(0)
const likeCount = ref(0)
const collectCount = ref(0)
const liked = ref(false)
const collected = ref(false)
const comments = ref<Comment[]>([])
const commentsLoading = ref(false)
const commentCount = computed(() =>
  comments.value.reduce((n, c) => n + 1 + c.replies.length, 0)
)

function onLike() {
  liked.value = !liked.value
  likeCount.value += liked.value ? 1 : -1
}
function onCollect() {
  collected.value = !collected.value
  collectCount.value += collected.value ? 1 : -1
}

function findComment(list: Comment[], targetId: string): Comment | undefined {
  for (const c of list) {
    if (c.id === targetId) return c
    const hit = c.replies.find((r) => r.id === targetId)
    if (hit) return hit
  }
  return undefined
}

async function onCommentSubmit(payload: { content: string; parentId?: string }) {
  await addComment({
    targetType: TARGET_TYPE,
    targetId: id,
    content: payload.content,
    ...(payload.parentId ? { parentId: payload.parentId } : {})
  })
  const fresh: Comment = {
    id: `local-${Date.now()}`,
    authorName: t('comment.me'),
    authorDept: '',
    content: payload.content,
    likeCount: 0,
    createdAt: new Date().toISOString(),
    replies: []
  }
  if (payload.parentId) {
    const parent = comments.value.find((c) => c.id === payload.parentId)
    if (parent) parent.replies.push(fresh)
  } else {
    comments.value.unshift(fresh)
  }
  message.success(t('comment.submitSuccess'))
}

async function onCommentLike(commentId: string) {
  await likeComment(commentId)
  const target = findComment(comments.value, commentId)
  if (target) target.likeCount += 1
}

function goBack() {
  router.push({ path: '/intel' })
}
function goIndustry(rid: string) {
  router.push({ path: '/intel/industry', query: { id: rid } })
  loadDetail(rid)
}

async function loadDetail(targetId: string) {
  detail.value = await getIndustryDetail(targetId)
  viewCount.value = detail.value.viewCount
  likeCount.value = detail.value.likeCount
  collectCount.value = detail.value.collectCount
  liked.value = false
  collected.value = false
  const all = await getIndustryList({ pageNumber: 1, pageSize: 999 })
  related.value = all.records
    .filter((d) => d.id !== targetId && d.industry === detail.value!.industry)
    .slice(0, 4)
  commentsLoading.value = true
  try {
    comments.value = await getComments(TARGET_TYPE, targetId)
  } finally {
    commentsLoading.value = false
  }
}

onMounted(() => loadDetail(id))
</script>

<style scoped>
.page-wrap {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px 16px 40px;
}
.nav-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  font-size: 14px;
  color: hsl(var(--secondary-text));
  cursor: pointer;
  transition: color 0.2s;
}
.nav-back:hover {
  color: hsl(var(--primary));
}
.hero-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 36px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}
.hero-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #1890ff, #36cfc9, #722ed1);
}
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.hero-title {
  font-size: 24px;
  font-weight: 700;
  color: hsl(var(--text));
  line-height: 1.45;
  margin-bottom: 18px;
}
.hero-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.hero-meta .divider {
  width: 1px;
  height: 14px;
  background: hsl(var(--line));
  padding: 0;
}
.content-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 36px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  font-size: 15px;
  color: hsl(var(--text));
  line-height: 1.9;
}
.summary-highlight {
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.08), rgba(54, 207, 201, 0.06));
  border-left: 4px solid #1890ff;
  padding: 16px 20px;
  border-radius: 0 12px 12px 0;
  margin-bottom: 24px;
  font-size: 15px;
  line-height: 1.8;
}
.summary-highlight strong {
  color: #1890ff;
}
.content-card h2 {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--text));
  margin: 24px 0 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid hsl(var(--line));
}
.content-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  margin: 18px 0 10px;
}
.content-card p {
  margin-bottom: 14px;
}
.key-points {
  padding-left: 22px;
  margin-bottom: 16px;
}
.key-points li {
  margin-bottom: 6px;
  line-height: 1.9;
}
.highlight-box {
  background: rgba(19, 194, 194, 0.08);
  border: 1px solid rgba(19, 194, 194, 0.3);
  border-radius: 10px;
  padding: 16px 20px;
  margin: 16px 0;
  font-size: 14px;
}
.highlight-box strong {
  color: #006d75;
}
.highlight-box ul {
  padding-left: 20px;
  margin: 8px 0 0;
}
.highlight-box li {
  margin-bottom: 4px;
  line-height: 1.8;
}
.source-ref {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid hsl(var(--line));
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.src-title {
  font-weight: 500;
  color: hsl(var(--text));
  margin-bottom: 2px;
}
.interact-card {
  background: #fff;
  border-radius: 16px;
  padding: 18px 36px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 18px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.related-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.related-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s;
}
.related-item:hover {
  border-color: hsl(var(--primary));
  box-shadow: 0 4px 16px hsl(var(--primary) / 0.08);
}
.related-icon {
  font-size: 20px;
  color: hsl(var(--primary));
  flex-shrink: 0;
}
.related-main {
  flex: 1;
  min-width: 0;
}
.related-title {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.related-sub {
  font-size: 11px;
  color: hsl(var(--secondary-text));
  margin-top: 2px;
}
.related-arrow {
  color: hsl(var(--secondary-text));
  flex-shrink: 0;
}
.page-footer {
  text-align: center;
  padding: 16px;
  color: hsl(var(--secondary-text));
  font-size: 12px;
  line-height: 1.8;
}
@media (max-width: 768px) {
  .related-grid {
    grid-template-columns: 1fr;
  }
  .hero-card,
  .content-card,
  .interact-card {
    padding-left: 20px;
    padding-right: 20px;
  }
}
</style>
