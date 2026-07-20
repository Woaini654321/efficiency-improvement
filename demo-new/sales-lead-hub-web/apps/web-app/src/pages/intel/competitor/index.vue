<template>
  <div v-if="!detail" class="h-full flex items-center justify-center bg-white rounded">
    <Empty type="noData" />
  </div>
  <div v-else class="page-wrap">
    <!-- 返回 -->
    <div class="nav-back" @click="goBack"><ArrowLeftOutlined /> {{ t('intel.backToCenter') }}</div>

    <!-- Hero 头部 -->
    <div class="hero-card comp">
      <div class="hero-tags">
        <a-tag color="purple">{{ detail.brand }}</a-tag>
        <a-tag v-if="detail.product">{{ detail.product }}</a-tag>
        <a-tag :color="intelTypeColor[detail.intelType] || 'default'">{{ t('dict.intelType.' + detail.intelType) }}</a-tag>
      </div>
      <h1 class="hero-title">{{ detail.title }}</h1>
      <div class="hero-meta">
        <span><LinkOutlined /> {{ t('intel.source') }}：{{ detail.source }}</span>
        <span class="divider" />
        <span><UserOutlined /> {{ detail.submitterName }}</span>
        <span class="divider" />
        <span><CalendarOutlined /> {{ (detail.createdAt || '').slice(0, 10) }}</span>
      </div>
    </div>

    <!-- 正文 -->
    <div class="content-card">
      <div class="summary-highlight comp">
        <strong>{{ t('intel.summaryLabel') }}</strong>{{ detail.summary }}
      </div>

      <h2>{{ t('intel.overviewComp') }}</h2>
      <p>{{ detail.overview }}</p>

      <h3>{{ t('intel.keyInfo') }}</h3>
      <table v-if="detail.specs.length" class="spec-table">
        <tbody>
          <tr v-for="(s, i) in detail.specs" :key="i">
            <td class="spec-label">{{ s.label }}</td>
            <td>{{ s.value }}</td>
          </tr>
        </tbody>
      </table>

      <h2>{{ t('intel.analysisComp') }}</h2>
      <p>{{ detail.analysis }}</p>

      <h2>{{ t('intel.impactComp') }}</h2>
      <p>{{ detail.impact }}</p>

      <!-- 情报类型专属应对建议 -->
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
          <div>{{ t('intel.submitter') }}：{{ detail.submitterName }}</div>
        </div>
      </div>
    </div>

    <!-- 互动栏 -->
    <div class="interact-card">
      <SocialStats
        :views="viewCount" :likes="likeCount" :collects="collectCount"
        :liked="liked" :collected="collected" interactive show-share :share-label="t('common.share')"
        @like="onLike" @collect="onCollect" @share="onShare"
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
      <div class="section-title"><RadarChartOutlined /> {{ t('intel.relatedComp') }}</div>
      <div class="related-grid">
        <div v-for="r in related" :key="r.id" class="related-item" @click="goCompetitor(r.id)">
          <a-tag :color="intelTypeColor[r.intelType] || 'default'">{{ t('dict.intelType.' + r.intelType) }}</a-tag>
          <div class="related-main">
            <div class="related-title">{{ r.title }}</div>
            <div class="related-sub">{{ r.brand }} · {{ (r.createdAt || '').slice(0, 10) }}</div>
          </div>
          <RightOutlined class="related-arrow" />
        </div>
      </div>
    </div>

    <!-- 页脚免责声明 -->
    <div class="page-footer">
      <div>{{ t('intel.footerProvided') }}</div>
      <div>{{ t('intel.footerDisclaimer') }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  ArrowLeftOutlined, LinkOutlined, UserOutlined, CalendarOutlined,
  MessageOutlined, RadarChartOutlined, RightOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import SocialStats from '@/components/social-stats/index.vue'
import CommentThread from '@/components/comment-thread/index.vue'
import { getCompetitorDetail, getCompetitorList } from '@/apis/intel/intelApi'
import { getComments, addComment, likeComment } from '@/apis/interaction/interactionApi'
import type { CompetitorIntelItem } from '@/apis/intel/types'
import type { Comment } from '@/apis/interaction/types'

defineOptions({ name: 'IntelCompetitorDetail' })
definePage({
  name: 'IntelCompetitorDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'intel.competitor'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<CompetitorIntelItem | null>(null)
const related = ref<CompetitorIntelItem[]>([])

const intelTypeColor: Record<string, string> = {
  new_product: 'green',
  price_change: 'orange',
  customer_case: 'blue',
  other: 'default'
}

// 情报类型专属应对建议
const adviceKey = computed(() => {
  const map: Record<string, string> = {
    new_product: 'adviceNew',
    price_change: 'advicePrice',
    customer_case: 'adviceCase'
  }
  return detail.value ? map[detail.value.intelType] || '' : ''
})
const adviceTitle = computed(() => (adviceKey.value ? t('intel.' + adviceKey.value + 'Title') : ''))
const adviceItems = computed(() =>
  adviceKey.value ? t('intel.' + adviceKey.value).split('\n').filter((l) => l.trim()) : []
)

// 互动
const TARGET_TYPE = 'CompetitorIntel'
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
function onShare() {
  message.success(t('common.share'))
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
  // 回读评论列表，拿后端真实 id/作者/时间，杜绝 local- 假 id 与假 parentId
  comments.value = await getComments(TARGET_TYPE, id)
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
function goCompetitor(rid: string) {
  router.push({ path: '/intel/competitor', query: { id: rid } })
  loadDetail(rid)
}

async function loadDetail(targetId: string) {
  detail.value = await getCompetitorDetail(targetId)
  viewCount.value = detail.value.viewCount
  likeCount.value = detail.value.likeCount
  collectCount.value = detail.value.collectCount
  liked.value = false
  collected.value = false
  // 相关推荐：同品牌或同类型，排除自身
  const all = await getCompetitorList({ pageNumber: 1, pageSize: 999 })
  related.value = all.records
    .filter((c) => c.id !== targetId && (c.brand === detail.value!.brand || c.intelType === detail.value!.intelType))
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
  background: linear-gradient(90deg, #722ed1, #1890ff, #36cfc9);
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
  background: hsl(var(--primary) / 0.06);
  border-left: 4px solid hsl(var(--primary));
  padding: 16px 20px;
  border-radius: 0 12px 12px 0;
  margin-bottom: 24px;
  font-size: 15px;
  line-height: 1.8;
}
.summary-highlight.comp {
  background: linear-gradient(135deg, rgba(114, 46, 209, 0.06), rgba(24, 144, 255, 0.06));
  border-left-color: #722ed1;
}
.summary-highlight strong {
  color: #722ed1;
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
.spec-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  margin: 14px 0;
  font-size: 13px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid hsl(var(--line));
}
.spec-table td {
  padding: 10px 14px;
  color: hsl(var(--text));
  border-bottom: 1px solid hsl(var(--line));
}
.spec-table tr:last-child td {
  border-bottom: none;
}
.spec-label {
  font-weight: 600;
  width: 32%;
  background: hsl(var(--card-bg));
}
.highlight-box {
  background: hsl(var(--primary) / 0.05);
  border: 1px solid hsl(var(--primary) / 0.2);
  border-radius: 10px;
  padding: 16px 20px;
  margin: 16px 0;
  font-size: 14px;
}
.highlight-box strong {
  color: hsl(var(--primary));
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
