<template>
  <div v-if="!detail" class="detail-empty">
    <Empty type="noData" />
  </div>

  <div v-else class="opp-detail">
    <!-- 过期 / 临期 提示横幅 -->
    <div v-if="isExpired" class="expiry-banner expired">
      <ExclamationCircleOutlined class="banner-icon" />
      <span class="flex-1">{{ t('opportunity.expiredBanner', { date: detail.expiryDate }) }}</span>
      <a v-if="detail.supersededBy" class="new-version-link" @click="goDetail(detail.supersededBy)">
        {{ t('opportunity.viewNewVersion') }} →
      </a>
    </div>
    <div v-else-if="isNearExpiry" class="expiry-banner near">
      <ClockCircleOutlined class="banner-icon" />
      <span>{{ t('opportunity.nearExpiryBanner', { date: detail.expiryDate, n: daysToExpiry }) }}</span>
    </div>

    <!-- Z1 面包屑 + 返回 -->
    <div class="crumb-row">
      <a-breadcrumb>
        <a-breadcrumb-item>
          <a @click="goHome">{{ t('opportunity.breadcrumbHome') }}</a>
        </a-breadcrumb-item>
        <a-breadcrumb-item>{{ t('dict.oppType.' + detail.type) }}</a-breadcrumb-item>
        <a-breadcrumb-item>{{ ellipsis(detail.title, 24) }}</a-breadcrumb-item>
      </a-breadcrumb>
      <a-button class="round-btn" @click="goHome">
        <template #icon><ArrowLeftOutlined /></template>
        {{ t('opportunity.backHome') }}
      </a-button>
    </div>

    <!-- Z2 Hero 卡片 -->
    <div class="hero-card">
      <div class="hero-blob blob-1" />
      <div class="hero-blob blob-2" />
      <div class="hero-inner">
        <div class="hero-tags">
          <a-tag :color="typeColor[detail.type] || 'default'" class="type-tag">
            {{ t('dict.oppType.' + detail.type) }}
          </a-tag>
          <a-tag v-if="currentStatus === 'archived'" color="error">{{ t('dict.oppStatus.archived') }}</a-tag>
          <a-tag v-else-if="currentStatus === 'published'" color="success">{{ t('dict.oppStatus.published') }}</a-tag>
        </div>

        <h1 class="hero-title">{{ detail.title }}</h1>

        <div class="hero-meta">
          <div class="publisher">
            <div class="publisher-avatar">{{ firstChar(detail.publisherName) }}</div>
            <div>
              <div class="publisher-name">{{ detail.publisherName }}</div>
              <div class="publisher-dept">{{ detail.publisherDeptName }}</div>
            </div>
          </div>
          <div class="meta-divider" />
          <div class="created-at">
            <ClockCircleOutlined /> {{ detail.createdAt }}
          </div>
        </div>

        <div class="hero-chips">
          <span v-for="c in detail.categoryNames" :key="c" class="cat-chip">{{ c }}</span>
        </div>

        <SocialStats
          class="hero-metrics"
          :views="detail.viewCount"
          :likes="likeCount"
          :collects="collectCount"
          :comments="totalCommentCount"
        />

        <div class="hero-divider" />

        <div class="hero-actions">
          <a-button class="round-btn" @click="goEdit">
            <template #icon><EditOutlined /></template>
            {{ t('common.edit') }}
          </a-button>
          <a-button class="round-btn" @click="goCopy">
            <template #icon><CopyOutlined /></template>
            {{ t('opportunity.copyAsNew') }}
          </a-button>
          <a-button v-if="currentStatus === 'published'" danger class="round-btn" @click="handleArchive">
            <template #icon><StopOutlined /></template>
            {{ t('opportunity.archive') }}
          </a-button>
          <a-button v-else type="primary" class="round-btn" @click="handleRepublish">
            <template #icon><CheckCircleOutlined /></template>
            {{ t('opportunity.restore') }}
          </a-button>
        </div>
      </div>
    </div>

    <!-- Z3 正文 -->
    <div class="detail-section">
      <div class="section-title"><FileTextOutlined /> {{ t('opportunity.content') }}</div>
      <!-- mock 富文本内容展示（后端接入后由富文本字段渲染） -->
      <div class="rich-body" v-html="detail.content"></div>
    </div>

    <!-- Z4 附件 -->
    <div v-if="detail.attachments.length" class="detail-section">
      <div class="section-title">
        <PaperClipOutlined /> {{ t('opportunity.attachments') }} ({{ detail.attachments.length }})
      </div>
      <QFileList :file-list="detail.attachments" :allow-delete="false" :show-download="true" />
    </div>

    <!-- Z5 互动栏 -->
    <div class="detail-section interaction-bar">
      <button class="social-pill" :class="{ liked }" @click="toggleLike">
        <HeartFilled v-if="liked" /><HeartOutlined v-else />
        {{ liked ? t('opportunity.liked') : t('common.like') }} {{ likeCount }}
      </button>
      <button class="social-pill" :class="{ collected }" @click="toggleCollect">
        <StarFilled v-if="collected" /><StarOutlined v-else />
        {{ collected ? t('opportunity.collected') : t('common.collect') }} {{ collectCount }}
      </button>
      <button class="social-pill" :class="{ collected: followed }" @click="toggleFollow">
        <BellFilled v-if="followed" /><BellOutlined v-else />
        {{ followed ? t('opportunity.followed') : t('common.follow') }}
      </button>
      <button class="social-pill" @click="shareOpen = true">
        <ShareAltOutlined /> {{ t('common.share') }}
      </button>
    </div>

    <!-- Z6 评论区 -->
    <div class="detail-section">
      <div class="section-title"><MessageOutlined /> {{ t('comment.headerLabel') }} ({{ totalCommentCount }})</div>

      <!-- 发表评论（含 @ 提及下拉建议） -->
      <div class="compose-box">
        <div class="compose-avatar">{{ firstChar(myName) }}</div>
        <div class="compose-main">
          <div class="mention-wrap">
            <a-textarea
              v-model:value="commentText"
              :rows="3"
              :maxlength="500"
              show-count
              :placeholder="t('comment.composePlaceholder')"
              @update:value="onComposeInput"
            />
            <div v-if="mentionOpen" class="mention-dropdown">
              <div v-if="!mentionCandidates.length" class="mention-empty">{{ t('comment.mentionEmpty') }}</div>
              <div
                v-for="(u, i) in mentionCandidates"
                :key="u"
                class="mention-item"
                @click="selectMention(u)"
              >
                <span class="mention-avatar" :style="{ background: avatarColor(i) }">{{ firstChar(u) }}</span>
                <span>{{ u }}</span>
              </div>
            </div>
          </div>
          <div class="compose-foot">
            <span class="mention-hint">{{ t('comment.mentionHint') }}</span>
            <a-button type="primary" class="round-btn" @click="handlePostComment">
              <template #icon><SendOutlined /></template>
              {{ t('comment.publishComment') }}
            </a-button>
          </div>
        </div>
      </div>

      <Empty v-if="!comments.length" type="noData" />

      <div v-else class="comment-list">
        <div v-for="(c, i) in displayComments" :key="c.id" class="comment-card">
          <button v-if="isMine(c)" class="del-btn" :title="t('common.delete')" @click="confirmDelete(c.id, false)">
            <DeleteOutlined />
          </button>
          <div class="cmt-head">
            <div class="cmt-avatar" :style="{ background: avatarColor(i) }">{{ firstChar(c.authorName) }}</div>
            <div class="cmt-body">
              <div class="cmt-meta">
                <span class="cmt-author">{{ c.authorName }}</span>
                <a-tag v-if="c.authorDept" class="dept-tag">{{ c.authorDept }}</a-tag>
                <span class="cmt-time">{{ relativeTime(c.createdAt) }}</span>
              </div>
              <div class="cmt-content">{{ c.content }}</div>
              <div class="cmt-ops">
                <button class="cmt-op" :class="{ liked: likedMap[c.id] }" @click="toggleCommentLike(c)">
                  <LikeFilled v-if="likedMap[c.id]" /><LikeOutlined v-else />
                  {{ c.likeCount > 0 ? c.likeCount : '' }}
                </button>
                <button class="cmt-op" @click="toggleReply(c.id)">
                  <MessageOutlined /> {{ t('comment.reply') }}
                </button>
              </div>

              <!-- 回复输入 -->
              <div v-if="replyingId === c.id" class="reply-input">
                <a-textarea
                  v-model:value="replyText"
                  :rows="2"
                  :maxlength="500"
                  :placeholder="t('comment.replyPlaceholder')"
                />
                <div class="reply-input-actions">
                  <a-button size="small" @click="cancelReply">{{ t('common.cancel') }}</a-button>
                  <a-button size="small" type="primary" @click="submitReply(c)">{{ t('comment.reply') }}</a-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 扁平回复列表（二级） -->
          <div v-if="c.replies.length" class="reply-list">
            <div v-for="r in c.replies" :key="r.id" class="reply-item" :class="{ mine: isMine(r) }">
              <div class="reply-meta">
                <span class="reply-author">{{ r.authorName }}</span>
                <a-tag v-if="r.authorDept" class="dept-tag">{{ r.authorDept }}</a-tag>
                <span class="cmt-time">{{ relativeTime(r.createdAt) }}</span>
              </div>
              <div class="reply-content">{{ r.content }}</div>
              <div class="reply-ops">
                <button class="cmt-op" @click="toggleReply(c.id)">
                  <MessageOutlined /> {{ t('comment.reply') }}
                </button>
                <button v-if="isMine(r)" class="cmt-op danger" @click="confirmDelete(r.id, true)">
                  <DeleteOutlined /> {{ t('common.delete') }}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="!showAll && comments.length > VISIBLE_COUNT" class="expand-more">
          <a-button type="link" @click="showAll = true">
            {{ t('comment.expandMore', { n: comments.length - VISIBLE_COUNT }) }}
          </a-button>
        </div>
      </div>
    </div>

    <!-- Z7 相关方案推荐 -->
    <div v-if="related.length" class="detail-section">
      <div class="section-title"><AppstoreOutlined /> {{ t('opportunity.relatedTitle') }}</div>
      <div class="related-grid">
        <div v-for="item in related" :key="item.id" class="related-item" @click="goDetail(item.id)">
          <a-tag :color="typeColor[item.type] || 'default'" class="rel-tag">{{ t('dict.oppType.' + item.type) }}</a-tag>
          <div class="rel-title">{{ item.title }}</div>
          <span class="rel-views"><EyeOutlined /> {{ item.viewCount }}</span>
        </div>
      </div>
    </div>

    <!-- 分享浮层 -->
    <div v-if="shareOpen" class="share-mask" @click="shareOpen = false">
      <div class="share-panel" @click.stop>
        <button class="share-close" @click="shareOpen = false"><CloseOutlined /></button>
        <div class="share-head">
          <div class="share-title">{{ t('opportunity.shareTitle') }}</div>
          <div class="share-sub">{{ detail.title }}</div>
        </div>
        <div class="share-grid">
          <div class="share-card" @click="shareFeishu">
            <TeamOutlined class="share-card-icon feishu" />
            <div class="share-card-title">{{ t('opportunity.shareFeishu') }}</div>
            <div class="share-card-desc">{{ t('opportunity.shareFeishuDesc') }}</div>
          </div>
          <div class="share-card" @click="shareCopyLink">
            <LinkOutlined class="share-card-icon" />
            <div class="share-card-title">{{ t('opportunity.shareCopyLink') }}</div>
            <div class="share-card-desc">{{ t('opportunity.shareCopyDesc') }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined, EditOutlined, CopyOutlined, StopOutlined, CheckCircleOutlined,
  ExclamationCircleOutlined, ClockCircleOutlined, FileTextOutlined, PaperClipOutlined,
  MessageOutlined, LikeOutlined, LikeFilled, StarOutlined, StarFilled,
  HeartOutlined, HeartFilled, BellOutlined, BellFilled, ShareAltOutlined,
  AppstoreOutlined, EyeOutlined, SendOutlined, DeleteOutlined, CloseOutlined, TeamOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import QFileList from '@/components/q-file-list/index.vue'
import SocialStats from '@/components/social-stats/index.vue'
import { getOpportunityDetail, getOpportunityList, changeOpportunityStatus } from '@/apis/opportunity/opportunityApi'
import { getComments, addComment, likeComment } from '@/apis/interaction/interactionApi'
import type { OpportunityItem } from '@/apis/opportunity/types'
import type { Comment } from '@/apis/interaction/types'

defineOptions({ name: 'OpportunityDetail' })
definePage({
  name: 'OpportunityDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'opportunity.detail'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<OpportunityItem | null>(null)

const TARGET_TYPE = 'Opportunity'
const VISIBLE_COUNT = 4
const AVATAR_COLORS = ['#f56a00', '#7265e6', '#ffbf00', '#00a2ae', '#87d068', '#1890ff', '#eb2f96', '#722ed1']
const typeColor: Record<string, string> = { product_info: 'blue', solution: 'green', success_case: 'orange' }

// @ 提及建议（展示层 mock 用户列表）
const MENTION_USERS = [
  'Tony.Zhang 张伟', 'Nina.Li 李娜', 'Kevin.Wang 王强', 'Yan.Chen 陈燕',
  'Leo.Zhao 赵磊', 'Owen.Liu 刘洋', 'Ben.Sun 孙八', 'Joe.Zhou 周九'
]

const myName = computed(() => t('comment.me'))

// ======== 过期治理 ========
function toTime(dateStr: string): number {
  if (!dateStr) return NaN
  return new Date(dateStr.replace(/-/g, '/')).getTime()
}
const isExpired = computed(() => {
  const d = detail.value
  if (!d || !d.expiryDate) return false
  const time = toTime(d.expiryDate)
  return !Number.isNaN(time) && time < Date.now()
})
const daysToExpiry = computed(() => {
  const d = detail.value
  if (!d || !d.expiryDate) return 0
  const time = toTime(d.expiryDate)
  if (Number.isNaN(time)) return 0
  return Math.ceil((time - Date.now()) / 86400000)
})
const isNearExpiry = computed(() => !isExpired.value && daysToExpiry.value >= 0 && daysToExpiry.value <= 30 && !!detail.value?.expiryDate)

// ======== 互动状态 ========
const currentStatus = ref('')
const likeCount = ref(0)
const collectCount = ref(0)
const liked = ref(false)
const collected = ref(false)
const followed = ref(false)
const shareOpen = ref(false)

function toggleLike() {
  liked.value = !liked.value
  likeCount.value += liked.value ? 1 : -1
  message.success(liked.value ? t('opportunity.liked') : t('common.success'))
}
function toggleCollect() {
  collected.value = !collected.value
  collectCount.value += collected.value ? 1 : -1
  message.success(collected.value ? t('opportunity.collected') : t('common.success'))
}
function toggleFollow() {
  followed.value = !followed.value
  message.success(followed.value ? t('opportunity.followSuccess') : t('opportunity.unfollowSuccess'))
}

function shareFeishu() {
  window.open('https://www.feishu.cn', '_blank')
  shareOpen.value = false
}
function shareCopyLink() {
  try {
    navigator.clipboard?.writeText(window.location.href)
  } catch { /* 展示层，忽略 */ }
  message.success(t('opportunity.shareCopySuccess'))
  shareOpen.value = false
}

// ======== 下架 / 重新上架（二次确认）========
function handleArchive() {
  Modal.confirm({
    title: t('opportunity.archiveConfirmTitle'),
    content: t('opportunity.archiveConfirmContent'),
    okText: t('opportunity.archiveConfirmOk'),
    cancelText: t('common.cancel'),
    okButtonProps: { danger: true },
    onOk: async () => {
      await changeOpportunityStatus(id, 'archived')
      currentStatus.value = 'archived'
      message.success(t('opportunity.archiveSuccess'))
    }
  })
}
function handleRepublish() {
  Modal.confirm({
    title: t('opportunity.republishTitle'),
    content: t('opportunity.republishContent'),
    okText: t('opportunity.republishOk'),
    cancelText: t('common.cancel'),
    onOk: async () => {
      await changeOpportunityStatus(id, 'published')
      currentStatus.value = 'published'
      message.success(t('opportunity.republishSuccess'))
    }
  })
}

// ======== 导航 ========
function goHome() {
  router.push({ path: '/opportunity' })
}
function goDetail(targetId: string) {
  if (!targetId) return
  router.push({ path: '/opportunity/detail', query: { id: targetId } })
}
function goEdit() {
  router.push({ path: '/opportunity/form', query: { id } })
}
function goCopy() {
  router.push({ path: '/opportunity/form', query: { copyFrom: id } })
}

// ======== 相关方案推荐（引用同一份 opportunity mock，取同类型其它方案）========
const related = ref<OpportunityItem[]>([])

// ======== 评论 ========
const comments = ref<Comment[]>([])
const commentText = ref('')
const replyText = ref('')
const replyingId = ref('')
const showAll = ref(false)
const likedMap = ref<Record<string, boolean>>({})
const mentionOpen = ref(false)
const mentionSearch = ref('')

const displayComments = computed(() => (showAll.value ? comments.value : comments.value.slice(0, VISIBLE_COUNT)))
const totalCommentCount = computed(() =>
  comments.value.reduce((sum, c) => sum + 1 + c.replies.length, 0)
)
const mentionCandidates = computed(() =>
  MENTION_USERS.filter((u) => u.toLowerCase().includes(mentionSearch.value.toLowerCase()))
)

function isMine(c: Comment): boolean {
  return c.authorName === myName.value
}
function firstChar(name: string): string {
  return name ? name.charAt(0) : '?'
}
function avatarColor(i: number): string {
  return AVATAR_COLORS[i % AVATAR_COLORS.length] || '#1890ff'
}
function ellipsis(text: string, len: number): string {
  return text.length > len ? text.slice(0, len) + '...' : text
}
function relativeTime(dateStr: string): string {
  if (!dateStr) return '--'
  const time = toTime(dateStr)
  if (Number.isNaN(time)) return dateStr
  const min = Math.floor((Date.now() - time) / 60000)
  if (min < 1) return t('comment.justNow')
  if (min < 60) return t('comment.minutesAgo', { n: min })
  const hour = Math.floor(min / 60)
  if (hour < 24) return t('comment.hoursAgo', { n: hour })
  const day = Math.floor(hour / 24)
  if (day < 30) return t('comment.daysAgo', { n: day })
  return dateStr.slice(0, 10)
}

function onComposeInput(val: string) {
  const at = val.lastIndexOf('@')
  if (at >= 0) {
    const after = val.slice(at + 1)
    if (!/[\s\n]/.test(after)) {
      mentionSearch.value = after
      mentionOpen.value = true
      return
    }
  }
  mentionOpen.value = false
}
function selectMention(name: string) {
  const at = commentText.value.lastIndexOf('@')
  commentText.value = commentText.value.slice(0, at) + '@' + name + ' '
  mentionOpen.value = false
  mentionSearch.value = ''
}

async function handlePostComment() {
  const content = commentText.value.trim()
  if (!content) {
    message.warning(t('comment.inputWarning'))
    return
  }
  await addComment({ targetType: TARGET_TYPE, targetId: id, content })
  comments.value.unshift({
    id: `local-${Date.now()}`,
    authorName: myName.value,
    authorDept: '',
    content,
    likeCount: 0,
    createdAt: new Date().toISOString(),
    replies: []
  })
  commentText.value = ''
  mentionOpen.value = false
  message.success(t('comment.submitSuccess'))
}

function toggleReply(cid: string) {
  replyingId.value = replyingId.value === cid ? '' : cid
  replyText.value = ''
}
function cancelReply() {
  replyingId.value = ''
  replyText.value = ''
}
async function submitReply(parent: Comment) {
  const content = replyText.value.trim()
  if (!content) return
  await addComment({ targetType: TARGET_TYPE, targetId: id, content, parentId: parent.id })
  parent.replies.push({
    id: `local-${Date.now()}`,
    authorName: myName.value,
    authorDept: '',
    content,
    likeCount: 0,
    createdAt: new Date().toISOString(),
    replies: []
  })
  cancelReply()
  message.success(t('comment.replySuccess'))
}

async function toggleCommentLike(c: Comment) {
  if (likedMap.value[c.id]) {
    likedMap.value[c.id] = false
    c.likeCount = Math.max(0, c.likeCount - 1)
    return
  }
  await likeComment(c.id)
  likedMap.value[c.id] = true
  c.likeCount += 1
}

function confirmDelete(targetId: string, isReply: boolean) {
  Modal.confirm({
    title: t(isReply ? 'comment.deleteReplyTitle' : 'comment.deleteTitle'),
    content: t(isReply ? 'comment.deleteReplyContent' : 'comment.deleteContent'),
    okText: t('comment.deleteOk'),
    cancelText: t('common.cancel'),
    okButtonProps: { danger: true },
    onOk: () => {
      comments.value = comments.value
        .filter((c) => c.id !== targetId)
        .map((c) => (c.replies.length ? { ...c, replies: c.replies.filter((r) => r.id !== targetId) } : c))
      message.success(t(isReply ? 'comment.deleteReplySuccess' : 'comment.deleteSuccess'))
    }
  })
}

onMounted(async () => {
  const d = await getOpportunityDetail(id)
  detail.value = d
  currentStatus.value = d.status
  likeCount.value = d.likeCount
  collectCount.value = d.collectCount

  comments.value = await getComments(TARGET_TYPE, id)

  const list = await getOpportunityList({ pageNumber: 1, pageSize: 999 })
  related.value = list.records
    .filter((it) => it.type === d.type && it.id !== d.id && it.status === 'published')
    .slice(0, 4)
})
</script>

<style scoped>
.detail-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 8px;
}
.opp-detail {
  height: 100%;
  overflow: auto;
  padding: 16px;
  max-width: 960px;
  margin: 0 auto;
}

/* 过期横幅 */
.expiry-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  border-radius: 10px;
  margin-bottom: 16px;
  font-size: 13px;
}
.expiry-banner.expired {
  background: hsl(var(--error) / 0.08);
  border: 1px solid hsl(var(--error) / 0.3);
  color: hsl(var(--error));
}
.expiry-banner.near {
  background: #fff7e6;
  border: 1px solid #ffd591;
  color: #ad8b00;
}
.banner-icon {
  font-size: 18px;
}
.new-version-link {
  color: hsl(var(--primary));
  cursor: pointer;
  white-space: nowrap;
  font-weight: 600;
}

/* 面包屑 */
.crumb-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.round-btn {
  border-radius: 10px;
}

/* Hero 卡片 */
.hero-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, hsl(var(--primary) / 0.06), hsl(var(--primary) / 0.02) 50%, #f6ffed);
  border: 1px solid hsl(var(--primary) / 0.15);
  border-radius: 16px;
  padding: 32px 36px;
  margin-bottom: 20px;
}
.hero-blob {
  position: absolute;
  border-radius: 50%;
}
.blob-1 {
  top: -60px;
  right: -60px;
  width: 220px;
  height: 220px;
  background: hsl(var(--primary) / 0.05);
}
.blob-2 {
  bottom: -40px;
  left: -40px;
  width: 160px;
  height: 160px;
  background: hsl(var(--primary) / 0.04);
}
.hero-inner {
  position: relative;
}
.hero-tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.type-tag {
  border-radius: 8px;
  font-weight: 600;
}
.hero-title {
  font-size: 26px;
  font-weight: 700;
  color: hsl(var(--text));
  line-height: 1.4;
  margin-bottom: 18px;
}
.hero-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.publisher {
  display: flex;
  align-items: center;
  gap: 10px;
}
.publisher-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: linear-gradient(135deg, hsl(var(--primary)), #36cfc9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  box-shadow: 0 3px 10px hsl(var(--primary) / 0.3);
}
.publisher-name {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
}
.publisher-dept {
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.meta-divider {
  width: 1px;
  height: 28px;
  background: hsl(var(--line));
}
.created-at {
  display: flex;
  align-items: center;
  gap: 5px;
  color: hsl(var(--secondary-text));
  font-size: 13px;
}
.hero-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.cat-chip {
  border-radius: 20px;
  background: hsl(var(--primary) / 0.08);
  color: hsl(var(--primary));
  border: 1px solid hsl(var(--primary) / 0.2);
  font-size: 12px;
  padding: 2px 12px;
}
.hero-metrics {
  margin-bottom: 4px;
}
.hero-divider {
  height: 1px;
  background: hsl(var(--line));
  margin: 16px 0 14px;
}
.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* 通用区块 */
.detail-section {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 20px;
}
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 16px;
}

/* 正文 */
.rich-body {
  line-height: 1.9;
  color: hsl(var(--text));
}
.rich-body :deep(h2) {
  font-size: 18px;
  font-weight: 700;
  margin: 20px 0 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid hsl(var(--line));
}
.rich-body :deep(h2):first-child {
  margin-top: 0;
}
.rich-body :deep(p) {
  color: hsl(var(--secondary-text));
  margin-bottom: 12px;
}

/* 互动栏 */
.interaction-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}
.social-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 20px;
  border-radius: 28px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid hsl(var(--line));
  background: #fff;
  font-size: 14px;
  color: hsl(var(--secondary-text));
  font-weight: 500;
}
.social-pill:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.social-pill.liked {
  border-color: hsl(var(--error));
  color: hsl(var(--error));
  background: hsl(var(--error) / 0.06);
}
.social-pill.collected {
  border-color: #faad14;
  color: #faad14;
  background: #fffbe6;
}

/* 评论 */
.compose-box {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  background: hsl(var(--card-bg));
  border-radius: 14px;
  padding: 16px;
}
.compose-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, hsl(var(--primary)), #36cfc9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}
.compose-main {
  flex: 1;
  min-width: 0;
}
.mention-wrap {
  position: relative;
}
.mention-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: calc(100% + 4px);
  z-index: 20;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  max-height: 220px;
  overflow: auto;
}
.mention-empty {
  padding: 12px;
  text-align: center;
  color: hsl(var(--secondary-text));
  font-size: 12px;
}
.mention-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 13px;
  color: hsl(var(--text));
}
.mention-item:hover {
  background: hsl(var(--primary) / 0.06);
}
.mention-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}
.compose-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.mention-hint {
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
.comment-list {
  display: flex;
  flex-direction: column;
}
.comment-card {
  position: relative;
  padding: 18px 0;
  border-bottom: 1px solid hsl(var(--line));
}
.comment-card:last-child {
  border-bottom: none;
}
.del-btn {
  position: absolute;
  top: 12px;
  right: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: hsl(var(--secondary-text));
}
.del-btn:hover {
  color: hsl(var(--error));
}
.cmt-head {
  display: flex;
  gap: 10px;
}
.cmt-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.cmt-body {
  flex: 1;
  min-width: 0;
}
.cmt-meta,
.reply-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.cmt-author {
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--text));
}
.reply-author {
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--text));
}
.dept-tag {
  font-size: 10px;
  margin: 0;
}
.cmt-time {
  font-size: 11px;
  color: hsl(var(--secondary-text));
  margin-left: auto;
}
.cmt-content {
  font-size: 14px;
  color: hsl(var(--text));
  line-height: 1.7;
  margin-bottom: 6px;
  white-space: pre-wrap;
  word-break: break-word;
}
.cmt-ops {
  display: flex;
  gap: 8px;
}
.cmt-op {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
  color: hsl(var(--secondary-text));
  font-size: 13px;
  border: none;
  background: transparent;
}
.cmt-op:hover {
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.06);
}
.cmt-op.liked {
  color: hsl(var(--primary));
}
.cmt-op.danger:hover {
  color: hsl(var(--error));
  background: hsl(var(--error) / 0.06);
}
.reply-input {
  margin-top: 8px;
}
.reply-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}
.reply-list {
  margin-left: 46px;
  margin-top: 8px;
}
.reply-item {
  padding: 10px 14px;
  margin-bottom: 4px;
  background: hsl(var(--card-bg));
  border-radius: 8px;
  border-left: 3px solid hsl(var(--line));
}
.reply-item.mine {
  background: #f6ffed;
  border-left-color: #b7eb8f;
}
.reply-content {
  font-size: 13px;
  color: hsl(var(--text));
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}
.reply-ops {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.expand-more {
  text-align: center;
  padding-top: 12px;
}

/* 相关推荐 */
.related-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}
.related-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.related-item:hover {
  border-color: hsl(var(--primary));
  box-shadow: 0 2px 8px hsl(var(--primary) / 0.1);
}
.rel-tag {
  flex-shrink: 0;
  margin: 0;
}
.rel-title {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rel-views {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: hsl(var(--secondary-text));
  flex-shrink: 0;
}

/* 分享浮层 */
.share-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}
.share-panel {
  position: relative;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  width: 100%;
  max-width: 440px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}
.share-close {
  position: absolute;
  top: 16px;
  right: 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: hsl(var(--secondary-text));
  font-size: 18px;
}
.share-head {
  text-align: center;
  margin-bottom: 24px;
}
.share-title {
  font-size: 18px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 4px;
}
.share-sub {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.share-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.share-card {
  cursor: pointer;
  padding: 24px 16px;
  border-radius: 12px;
  border: 1px solid hsl(var(--line));
  text-align: center;
  transition: all 0.2s;
}
.share-card:hover {
  border-color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.05);
}
.share-card-icon {
  font-size: 32px;
  color: hsl(var(--primary));
  display: block;
  margin-bottom: 8px;
}
.share-card-icon.feishu {
  color: #3370ff;
}
.share-card-title {
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 2px;
}
.share-card-desc {
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
</style>
