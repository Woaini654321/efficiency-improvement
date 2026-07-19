<template>
  <div v-if="!detail" class="h-full p-[16px] bg-white rounded flex items-center justify-center">
    <Empty type="noData" />
  </div>
  <div v-else class="req-detail">
    <!-- 顶部：面包屑 + 返回 -->
    <div class="detail-topbar">
      <a-breadcrumb>
        <a-breadcrumb-item><a @click="router.push('/requirement')">{{ t('requirement.squareTitle') }}</a></a-breadcrumb-item>
        <a-breadcrumb-item>{{ ellipsis(detail.title, 24) }}</a-breadcrumb-item>
      </a-breadcrumb>
      <a-button @click="router.push('/requirement')">
        <template #icon><ArrowLeftOutlined /></template>{{ t('requirement.backList') }}
      </a-button>
    </div>

    <!-- Z2 基本信息卡 -->
    <div class="card">
      <div class="flex items-start justify-between gap-3">
        <div class="flex-1 min-w-0">
          <div class="title-line">
            <h2 class="detail-title">{{ detail.title }}</h2>
            <a-tag :color="urgencyColor[detail.urgency] || 'default'" class="font-semibold">{{ t('dict.urgency.' + detail.urgency) }}</a-tag>
            <a-tag :color="statusColor[detail.status] || 'default'">{{ t('dict.reqStatus.' + detail.status) }}</a-tag>
            <a-tag v-if="countdown.label" :color="countdown.color">{{ countdown.label }}</a-tag>
          </div>
          <div class="meta-line">
            <span><UserOutlined /> {{ detail.publisherName }} · {{ detail.publisherDeptName }}</span>
            <span><ClockCircleOutlined /> {{ detail.createdAt }}</span>
            <span><BankOutlined /> {{ detail.industry }}</span>
            <span><EyeOutlined /> {{ detail.viewCount }}</span>
            <span><LikeOutlined /> {{ likeCount }}</span>
            <span><StarOutlined /> {{ collectCount }}</span>
          </div>
          <div class="scope-line">
            <span class="scope-item">
              <EyeOutlined />
              <b>{{ t('requirement.visibility') }}：</b>
              <a-tag v-if="detail.visibilityType === 'all'" color="green">{{ t('requirement.visibilityType.all') }}</a-tag>
              <template v-else>
                <a-tag :color="detail.visibilityType === 'dept' ? 'blue' : 'purple'"
                  v-for="v in detail.visibilityValues" :key="v">{{ v }}</a-tag>
              </template>
            </span>
            <span class="scope-item">
              <UsergroupAddOutlined />
              <b>{{ t('requirement.invitedProductLines') }}：</b>
              <template v-if="detail.invitedProductLines.length">
                <a-tag v-for="p in detail.invitedProductLines" :key="p.name" :color="p.responded ? 'green' : 'default'">
                  {{ p.name }}<template v-if="!p.responded"> · {{ t('requirement.waitResponse') }}</template>
                </a-tag>
              </template>
              <span v-else class="text-[hsl(var(--secondary-text))]">{{ t('requirement.autoMatch') }}</span>
            </span>
          </div>
        </div>
        <a-button v-if="detail.status !== 'Closed'" danger @click="handleClose">
          <template #icon><CloseCircleOutlined /></template>{{ t('requirement.close') }}
        </a-button>
      </div>
    </div>

    <!-- Z3 需求描述 -->
    <div class="card">
      <h3 class="card-title">{{ t('requirement.description') }}</h3>
      <div class="rich-body" v-html="detail.description"></div>
    </div>

    <!-- Z3.5 邀请回答进度 -->
    <div v-if="detail.invitedProductLines.length" class="card invite-card"
      :class="{ done: respondedCount === detail.invitedProductLines.length }">
      <div class="card-title flex items-center gap-2">
        <UsergroupAddOutlined />
        <span>{{ t('requirement.inviteProgress') }}</span>
        <a-tag :color="respondedCount === detail.invitedProductLines.length ? 'success' : 'processing'">
          {{ respondedCount }}/{{ detail.invitedProductLines.length }} {{ t('requirement.responded') }}
        </a-tag>
      </div>
      <div v-for="p in detail.invitedProductLines" :key="p.name" class="invite-row">
        <div class="flex items-center gap-2">
          <CheckCircleOutlined v-if="p.responded" class="text-[#52c41a]" />
          <ClockCircleOutlined v-else class="text-[hsl(var(--line))]" />
          <span class="font-medium">{{ p.name }}</span>
          <a-tag v-if="p.responded" color="success">{{ t('requirement.respondedN', { n: p.responderCount }) }}</a-tag>
          <a-tag v-else color="default">{{ t('requirement.waitResponse') }}</a-tag>
        </div>
        <a-button v-if="!p.responded" type="link" size="small" @click="handleNudge(p.name)">
          <template #icon><BellOutlined /></template>{{ t('requirement.nudge') }}
        </a-button>
      </div>
    </div>

    <!-- Z4 方案响应 -->
    <div class="card">
      <div class="card-title flex items-center justify-between">
        <span>{{ t('requirement.responses') }}（{{ solutions.length }}）</span>
        <a-button v-if="detail.status !== 'Closed'" type="primary" @click="drawerOpen = true">
          <template #icon><FileTextOutlined /></template>{{ t('requirement.submitSolution') }}
        </a-button>
      </div>
      <div v-if="solutions.length" class="flex flex-col gap-3">
        <div v-for="s in solutions" :key="s.id" class="solution-card" :class="{ best: s.isAdopted }">
          <div v-if="s.isAdopted" class="best-badge"><CrownOutlined /> {{ t('requirement.bestSolution') }}</div>
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap mb-1">
                <a-avatar :size="24" :style="{ background: s.isAdopted ? '#faad14' : 'hsl(var(--primary))' }">{{ firstChar(s.responderName) }}</a-avatar>
                <span class="font-semibold">{{ s.responderName }}</span>
                <span class="text-[hsl(var(--secondary-text))] text-[12px]">{{ s.responderDeptName }}</span>
                <a-tag v-if="s.productLineName && detail.invitedProductLineNames.includes(s.productLineName)" color="purple">
                  <UsergroupAddOutlined /> {{ t('requirement.invited') }}
                </a-tag>
                <span class="text-[hsl(var(--secondary-text))] text-[12px] ml-auto">{{ s.createdAt }}</span>
              </div>
              <div class="solution-content">
                <template v-for="(seg, i) in splitMentions(s.content)" :key="i">
                  <span v-if="seg.mention" class="mention">{{ seg.text }}</span><template v-else>{{ seg.text }}</template>
                </template>
              </div>
              <div v-if="s.files.length" class="flex gap-2 flex-wrap mt-2">
                <a-tag v-for="f in s.files" :key="f" color="processing"><PaperClipOutlined /> {{ f }}</a-tag>
              </div>
            </div>
            <a-button v-if="!s.isAdopted && detail.status !== 'Closed'" type="primary" ghost size="small" @click="handleAdopt(s.id)">
              <CheckCircleOutlined /> {{ t('requirement.adoptBest') }}
            </a-button>
          </div>
        </div>
      </div>
      <Empty v-else type="noData" />
    </div>

    <!-- Z5 互动栏（点赞/收藏/关注/分享） -->
    <div class="card">
      <div class="interaction-row">
        <button class="pill" :class="{ active: liked }" @click="onLike"><LikeFilled v-if="liked" /><LikeOutlined v-else /> {{ likeCount }}</button>
        <button class="pill" :class="{ active: collected }" @click="onCollect"><StarFilled v-if="collected" /><StarOutlined v-else /> {{ collected ? t('requirement.collected') : t('common.collect') }}</button>
        <button class="pill" :class="{ active: followed }" @click="onFollow"><BellFilled v-if="followed" /><BellOutlined v-else /> {{ followed ? t('requirement.followed') : t('common.follow') }}</button>
        <button class="pill" @click="shareOpen = true"><ShareAltOutlined /> {{ t('common.share') }}</button>
      </div>
    </div>

    <!-- Z6 评论区（@提及 + 删除） -->
    <div class="card">
      <h3 class="card-title">{{ t('comment.section') }}（{{ commentTotal }}）</h3>
      <div class="comment-input">
        <a-textarea :value="commentDraft" :rows="2" :maxlength="500" :placeholder="t('requirement.commentPlaceholder')"
          @update:value="(v: any) => (commentDraft = v ?? '')" />
        <div class="mention-chips">
          <span class="mention-hint">{{ t('requirement.mention') }}：</span>
          <a-tag v-for="u in mentionUsers" :key="u" class="mention-chip" @click="insertMention('comment', u)">@{{ u }}</a-tag>
        </div>
        <div class="flex justify-end">
          <a-button type="primary" @click="submitComment"><template #icon><SendOutlined /></template>{{ t('comment.publish') }}</a-button>
        </div>
      </div>

      <a-divider class="my-3" />

      <Empty v-if="!comments.length" type="noData" />
      <div v-else class="flex flex-col gap-4">
        <div v-for="(c, ci) in comments" :key="c.id" class="comment-card">
          <div class="flex gap-3">
            <a-avatar :style="{ background: avatarColor(ci) }">{{ firstChar(c.author) }}</a-avatar>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-semibold">{{ c.author }}</span>
                <a-tag>{{ c.dept }}</a-tag>
                <span class="text-[12px] text-[hsl(var(--secondary-text))] ml-auto">{{ c.time }}</span>
              </div>
              <div v-if="c.deleted" class="comment-content text-[hsl(var(--secondary-text))] italic">{{ t('comment.deletedPlaceholder') }}</div>
              <template v-else>
                <div class="comment-content">
                  <template v-for="(seg, i) in splitMentions(c.content)" :key="i">
                    <span v-if="seg.mention" class="mention">{{ seg.text }}</span><template v-else>{{ seg.text }}</template>
                  </template>
                </div>
                <div class="comment-ops">
                  <span class="op" @click="toggleReply(c.id)"><MessageOutlined /> {{ t('comment.reply') }}</span>
                  <span v-if="c.isMine" class="op danger" @click="deleteComment(c.id)"><DeleteOutlined /> {{ t('common.delete') }}</span>
                </div>
              </template>

              <div v-if="replyingId === c.id" class="reply-input">
                <a-textarea :value="replyDraft" :rows="2" :maxlength="500" :placeholder="t('comment.replyPlaceholder')"
                  @update:value="(v: any) => (replyDraft = v ?? '')" />
                <div class="flex justify-end gap-2 mt-2">
                  <a-button size="small" @click="cancelReply">{{ t('common.cancel') }}</a-button>
                  <a-button size="small" type="primary" @click="submitReply(c.id)">{{ t('comment.publish') }}</a-button>
                </div>
              </div>

              <div v-if="c.replies.length" class="reply-list">
                <div v-for="r in c.replies" :key="r.id" class="reply-item" :class="{ mine: r.isMine }">
                  <div class="flex items-center gap-2 flex-wrap">
                    <span class="font-semibold text-[13px]">{{ r.author }}</span>
                    <a-tag>{{ r.dept }}</a-tag>
                    <span class="text-[11px] text-[hsl(var(--secondary-text))] ml-auto">{{ r.time }}</span>
                  </div>
                  <div v-if="r.deleted" class="comment-content text-[13px] text-[hsl(var(--secondary-text))] italic">{{ t('comment.deletedPlaceholder') }}</div>
                  <template v-else>
                    <div class="comment-content text-[13px]">
                      <template v-for="(seg, i) in splitMentions(r.content)" :key="i">
                        <span v-if="seg.mention" class="mention">{{ seg.text }}</span><template v-else>{{ seg.text }}</template>
                      </template>
                    </div>
                    <div v-if="r.isMine" class="comment-ops">
                      <span class="op danger" @click="deleteReply(c.id, r.id)"><DeleteOutlined /> {{ t('common.delete') }}</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- DRAWER 提交方案 -->
    <a-drawer :open="drawerOpen" :title="t('requirement.submitSolution')" :width="720" @close="drawerOpen = false">
      <template #extra>
        <a-space>
          <a-button @click="drawerOpen = false">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" @click="submitSolution">{{ t('common.submit') }}</a-button>
        </a-space>
      </template>

      <div class="drawer-section">
        <div class="drawer-label">{{ t('requirement.solutionContent') }}</div>
        <RichEditor v-model:modelValue="solutionContent" :placeholder="t('requirement.solutionPlaceholder')" />
        <div class="mention-chips mt-2">
          <span class="mention-hint">{{ t('requirement.mention') }}：</span>
          <a-tag v-for="u in mentionUsers" :key="u" class="mention-chip" @click="insertMention('solution', u)">@{{ u }}</a-tag>
        </div>
      </div>

      <div class="drawer-section">
        <div class="drawer-label">{{ t('requirement.attachments') }} <span class="text-[hsl(var(--secondary-text))] text-[12px] font-normal">（{{ t('requirement.attachHint') }}）</span></div>
        <QUpload type="dragger" manual multiple :max-count="10" :max-file-size="50"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.csv,.zip,.rar"
          @update:value="(v: any) => (solutionFiles = v)" />
      </div>

      <div class="notify-card">
        <div class="drawer-label flex items-center gap-1"><BellOutlined /> {{ t('requirement.notifySettings') }}</div>
        <div class="mb-3">
          <div class="flex items-center gap-2 mb-2">
            <MailOutlined class="text-[hsl(var(--primary))]" />
            <b class="text-[13px]">{{ t('requirement.emailNotify') }}</b>
            <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('requirement.emailNotifyHint') }}</span>
          </div>
          <a-checkbox-group v-model:value="emailRecipients" :options="emailOptions" />
          <div class="mt-3">
            <a-select mode="tags" class="w-full" :value="customEmails" :placeholder="t('requirement.customEmailPlaceholder')"
              :options="personnelEmailOptions" :token-separators="[',', ';', ' ']"
              @update:value="(v: any) => (customEmails = v ?? [])" />
          </div>
        </div>
        <a-divider class="my-3" />
        <div class="flex items-center justify-between">
          <div>
            <b class="text-[13px]">{{ t('requirement.feishuSync') }}</b>
            <div class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('requirement.feishuSyncHint') }}</div>
          </div>
          <a-switch v-model:checked="feishuSync" />
        </div>
        <div v-if="feishuSync" class="feishu-tip"><InfoCircleOutlined /> {{ t('requirement.feishuTip') }}</div>
      </div>
    </a-drawer>

    <!-- 分享浮层 -->
    <div v-if="shareOpen" class="share-mask" @click.self="shareOpen = false">
      <div class="share-panel">
        <button class="share-close" @click="shareOpen = false"><CloseOutlined /></button>
        <div class="share-head">
          <div class="share-title">{{ t('requirement.shareTitle') }}</div>
          <div class="share-sub">{{ ellipsis(detail.title, 30) }}</div>
        </div>
        <div class="share-grid">
          <div class="share-opt" @click="doShare('feishu')">
            <TeamOutlined class="share-ic" />
            <div class="share-opt-title">{{ t('requirement.shareFeishu') }}</div>
            <div class="share-opt-sub">{{ t('requirement.shareFeishuSub') }}</div>
          </div>
          <div class="share-opt" @click="doShare('link')">
            <LinkOutlined class="share-ic" />
            <div class="share-opt-title">{{ t('requirement.shareLink') }}</div>
            <div class="share-opt-sub">{{ t('requirement.shareLinkSub') }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  ArrowLeftOutlined, UserOutlined, ClockCircleOutlined, BankOutlined, EyeOutlined,
  LikeOutlined, LikeFilled, StarOutlined, StarFilled, BellOutlined, BellFilled,
  ShareAltOutlined, MessageOutlined, CrownOutlined, PaperClipOutlined, UsergroupAddOutlined,
  CloseCircleOutlined, CheckCircleOutlined, FileTextOutlined, SendOutlined, DeleteOutlined,
  MailOutlined, InfoCircleOutlined, CloseOutlined, LinkOutlined, TeamOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import RichEditor from '@/components/rich-editor/index.vue'
import QUpload from '@/components/q-upload/index.vue'
import { getRequirementDetail } from '@/apis/requirement/requirementApi'
import type { RequirementItem, RequirementResponseItem } from '@/apis/requirement/types'
import options from '@/apis/requirement/mocks/requirementOptions.json'

defineOptions({ name: 'RequirementDetail' })
definePage({
  name: 'RequirementDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'requirement.detail'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<RequirementItem | null>(null)

const urgencyColor: Record<string, string> = { critical: 'red', urgent: 'orange', normal: 'green' }
const statusColor: Record<string, string> = { Pending: 'orange', Collecting: 'blue', Adopted: 'green', Closed: 'default' }

const mentionUsers = options.mentionUsers
const ME_NAME = options.meName
const ME_DEPT = options.meDept
const AVATAR_COLORS = ['#1890ff', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']

// ===== SLA 首响倒计时（每秒 tick，仅待响应状态显示） =====
// 剩余 = createdAt + slaHours(urgency) - now；首响时限：critical 2h / urgent 4h / normal 24h
const SLA_HOURS: Record<string, number> = { critical: 2, urgent: 4, normal: 24 }
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null
const countdown = computed<{ label: string; color: string }>(() => {
  const d = detail.value
  if (!d) return { label: '', color: 'default' }
  // 非待响应状态不显示倒计时，改显已响应/已闭环
  if (d.status !== 'Pending') {
    if (d.status === 'Closed') return { label: t('requirement.slaClosed'), color: 'default' }
    return { label: t('requirement.responded'), color: 'green' }
  }
  const start = new Date((d.createdAt || '').replace(/-/g, '/')).getTime()
  if (Number.isNaN(start)) return { label: '', color: 'default' }
  const slaH = SLA_HOURS[d.urgency] ?? 24
  const diff = start + slaH * 3600000 - now.value
  if (diff <= 0) return { label: t('requirement.slaOverdue'), color: 'red' }
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  // 变色阈值：临近(剩余<1h) 橙色告警，正常绿色
  const color = diff < 3600000 ? 'orange' : 'green'
  return { label: t('requirement.slaRemain', { h, m, s }), color }
})

// ===== 互动状态 =====
const liked = ref(false)
const collected = ref(false)
const followed = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const shareOpen = ref(false)

function onLike() {
  liked.value = !liked.value
  likeCount.value += liked.value ? 1 : -1
  message.success(liked.value ? t('requirement.likeOk') : t('requirement.likeCancel'))
}
function onCollect() {
  collected.value = !collected.value
  collectCount.value += collected.value ? 1 : -1
  message.success(collected.value ? t('requirement.collectOk') : t('requirement.collectCancel'))
}
function onFollow() {
  followed.value = !followed.value
  message.success(followed.value ? t('requirement.followOk') : t('requirement.followCancel'))
}
function doShare(kind: 'feishu' | 'link') {
  if (kind === 'link') message.success(t('requirement.linkCopied'))
  else message.success(t('requirement.shareFeishuOk'))
  shareOpen.value = false
}

// ===== 方案响应 =====
const solutions = ref<RequirementResponseItem[]>([])
const respondedCount = computed(() => detail.value?.invitedProductLines.filter((p) => p.responded).length ?? 0)

function handleAdopt(sid: string) {
  Modal.confirm({
    title: t('requirement.adoptConfirmTitle'),
    content: t('requirement.adoptConfirmContent'),
    okText: t('requirement.adoptConfirmOk'),
    cancelText: t('common.cancel'),
    onOk: () => {
      solutions.value = solutions.value.map((s) => ({ ...s, isAdopted: s.id === sid }))
      message.success(t('requirement.adoptOk'))
    }
  })
}
function handleNudge(name: string) {
  Modal.confirm({
    title: t('requirement.nudgeTitle'),
    content: t('requirement.nudgeContent', { name }),
    okText: t('requirement.nudgeOk'),
    cancelText: t('common.cancel'),
    onOk: () => message.success(t('requirement.nudgeSent', { name }))
  })
}
function handleClose() {
  Modal.confirm({
    title: t('requirement.closeTitle'),
    content: t('requirement.closeContent'),
    okText: t('requirement.closeOk'),
    okButtonProps: { danger: true },
    cancelText: t('common.cancel'),
    onOk: () => message.success(t('requirement.closeOk'))
  })
}

// ===== 提交方案抽屉 =====
const drawerOpen = ref(false)
const solutionContent = ref('')
const solutionFiles = ref<any[]>([])
const emailRecipients = ref<string[]>(['publisher'])
const customEmails = ref<string[]>([])
const feishuSync = ref(true)

const emailOptions = computed(() => [
  { label: t('requirement.emailPublisher'), value: 'publisher' },
  { label: t('requirement.emailResponders'), value: 'responders' },
  { label: t('requirement.emailFollowers'), value: 'followers' }
])
const personnelEmailOptions = options.personnelEmailOptions

function submitSolution() {
  const text = solutionContent.value.replace(/<[^>]+>/g, '').trim()
  if (!text) { message.warning(t('requirement.solutionRequired')); return }
  if (!solutionFiles.value.length) { message.warning(t('requirement.attachRequired')); return }
  const parts = [t('requirement.solutionSubmitted')]
  if (emailRecipients.value.length || customEmails.value.length) parts.push(t('requirement.emailSent'))
  if (feishuSync.value) parts.push(t('requirement.feishuSent'))
  message.success(parts.join('；'))
  drawerOpen.value = false
  solutionContent.value = ''
  solutionFiles.value = []
}

// ===== @提及插入 =====
function insertMention(target: 'comment' | 'solution', user: string) {
  if (target === 'comment') commentDraft.value += `@${user} `
  else solutionContent.value += `@${user}&nbsp;`
}
function splitMentions(text: string): { text: string; mention: boolean }[] {
  if (!text) return [{ text: '', mention: false }]
  const parts = text.split(/(@\S+)/g)
  return parts.filter((p) => p !== '').map((p) => ({
    text: p,
    mention: p.charAt(0) === '@' && mentionUsers.some((u) => `@${u}` === p)
  }))
}

// ===== 评论区（本地 mock） =====
interface ReplyVM { id: string; author: string; dept: string; content: string; time: string; isMine: boolean; deleted: boolean }
interface CommentVM extends ReplyVM { replies: ReplyVM[] }
const comments = ref<CommentVM[]>([])
const commentDraft = ref('')
const replyDraft = ref('')
const replyingId = ref('')
const commentTotal = computed(() => comments.value.reduce((n, c) => n + 1 + c.replies.length, 0))

function seedComments() {
  comments.value = options.seedComments.map((c) => ({
    id: c.id, author: c.author, dept: c.dept, content: c.content, time: c.time, isMine: c.isMine, deleted: false,
    replies: c.replies.map((r) => ({ id: r.id, author: r.author, dept: r.dept, content: r.content, time: r.time, isMine: r.isMine, deleted: false }))
  }))
}

function submitComment() {
  const content = commentDraft.value.trim()
  if (!content) { message.warning(t('requirement.commentRequired')); return }
  comments.value.unshift({
    id: `local-${Date.now()}`, author: ME_NAME, dept: ME_DEPT,
    content, time: nowStr(), isMine: true, deleted: false, replies: []
  })
  commentDraft.value = ''
  message.success(t('comment.submitSuccess'))
}
function toggleReply(cid: string) {
  replyingId.value = replyingId.value === cid ? '' : cid
  replyDraft.value = ''
}
function cancelReply() {
  replyingId.value = ''
  replyDraft.value = ''
}
function submitReply(cid: string) {
  const content = replyDraft.value.trim()
  if (!content) return
  const parent = comments.value.find((c) => c.id === cid)
  if (parent) parent.replies.push({ id: `local-${Date.now()}`, author: ME_NAME, dept: ME_DEPT, content, time: nowStr(), isMine: true, deleted: false })
  cancelReply()
  message.success(t('comment.submitSuccess'))
}
function deleteComment(cid: string) {
  Modal.confirm({
    title: t('requirement.delCommentTitle'),
    okText: t('common.delete'), okButtonProps: { danger: true }, cancelText: t('common.cancel'),
    onOk: () => {
      const target = comments.value.find((c) => c.id === cid)
      if (target) target.deleted = true
      message.success(t('requirement.delOk'))
    }
  })
}
function deleteReply(cid: string, rid: string) {
  Modal.confirm({
    title: t('requirement.delReplyTitle'),
    okText: t('common.delete'), okButtonProps: { danger: true }, cancelText: t('common.cancel'),
    onOk: () => {
      const parent = comments.value.find((c) => c.id === cid)
      const target = parent?.replies.find((r) => r.id === rid)
      if (target) target.deleted = true
      message.success(t('requirement.delOk'))
    }
  })
}

// ===== 工具 =====
function firstChar(name: string): string { return name ? name.charAt(0) : '?' }
function avatarColor(i: number): string { return AVATAR_COLORS[i % AVATAR_COLORS.length] || '#1890ff' }
function ellipsis(s: string, n: number): string { return s.length > n ? s.slice(0, n) + '...' : s }
function nowStr(): string {
  const d = new Date()
  const p = (x: number) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(async () => {
  detail.value = await getRequirementDetail(id)
  solutions.value = detail.value.responses.map((r) => ({ ...r }))
  likeCount.value = detail.value.likeCount
  collectCount.value = detail.value.collectCount
  seedComments()
  timer = setInterval(() => { now.value = Date.now() }, 1000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.req-detail {
  padding: 16px;
  height: 100%;
  overflow: auto;
}
.detail-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 12px;
}
.title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--text));
  margin: 0;
}
.meta-line {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.meta-line span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.scope-line {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid hsl(var(--line));
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.scope-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.rich-body {
  line-height: 1.8;
  color: hsl(var(--text));
}
.rich-body :deep(h2) {
  font-size: 16px;
  font-weight: 600;
  margin: 12px 0 8px;
}
.rich-body :deep(ul) {
  padding-left: 22px;
}
.invite-card {
  border-color: #ffe58f;
}
.invite-card.done {
  border-color: #b7eb8f;
}
.invite-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid hsl(var(--line));
}
.invite-row:last-child {
  border-bottom: none;
}
.solution-card {
  position: relative;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  padding: 14px 16px;
}
.solution-card.best {
  border-color: #faad14;
  background: #fffbe6;
}
.best-badge {
  position: absolute;
  top: -1px;
  right: -1px;
  background: linear-gradient(135deg, #faad14, #fa8c16);
  color: #fff;
  padding: 2px 12px;
  border-radius: 0 9px 0 8px;
  font-size: 12px;
  font-weight: 600;
}
.solution-content,
.comment-content {
  color: hsl(var(--text));
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.mention {
  color: hsl(var(--primary));
  font-weight: 500;
  background: hsl(var(--primary) / 0.06);
  border-radius: 2px;
  padding: 0 2px;
}
.interaction-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 18px;
  border: 1px solid hsl(var(--line));
  border-radius: 999px;
  background: hsl(var(--card-bg));
  color: hsl(var(--text));
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.pill:hover {
  border-color: hsl(var(--primary));
  color: hsl(var(--primary));
}
.pill.active {
  border-color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.1);
  color: hsl(var(--primary));
}
.comment-input {
  background: hsl(var(--card-bg));
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  padding: 12px;
}
.mention-chips {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin: 8px 0;
}
.mention-hint {
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.mention-chip {
  cursor: pointer;
  margin: 0;
}
.comment-card {
  padding-bottom: 16px;
  border-bottom: 1px solid hsl(var(--line));
}
.comment-card:last-child {
  border-bottom: none;
}
.comment-content {
  margin: 6px 0;
}
.comment-ops {
  display: flex;
  gap: 16px;
  margin-top: 4px;
}
.op {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
  cursor: pointer;
}
.op:hover {
  color: hsl(var(--primary));
}
.op.danger:hover {
  color: hsl(var(--error));
}
.reply-input {
  margin-top: 10px;
}
.reply-list {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.reply-item {
  padding: 10px 12px;
  background: hsl(var(--card-bg));
  border-radius: 8px;
  border-left: 3px solid hsl(var(--line));
}
.reply-item.mine {
  border-left-color: #b7eb8f;
}
.drawer-section {
  margin-bottom: 18px;
}
.drawer-label {
  font-weight: 600;
  font-size: 14px;
  color: hsl(var(--text));
  margin-bottom: 8px;
}
.notify-card {
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  padding: 14px 16px;
}
.feishu-tip {
  margin-top: 12px;
  padding: 8px 12px;
  background: hsl(var(--card-bg));
  border-radius: 6px;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
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
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  width: 100%;
  max-width: 440px;
  position: relative;
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
}
.share-sub {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin-top: 4px;
}
.share-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.share-opt {
  cursor: pointer;
  padding: 24px 16px;
  border-radius: 12px;
  border: 1px solid hsl(var(--line));
  text-align: center;
  transition: all 0.2s;
}
.share-opt:hover {
  border-color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.06);
}
.share-ic {
  font-size: 30px;
  color: hsl(var(--primary));
  margin-bottom: 8px;
}
.share-opt-title {
  font-size: 14px;
  font-weight: 600;
  color: hsl(var(--text));
}
.share-opt-sub {
  font-size: 11px;
  color: hsl(var(--secondary-text));
  margin-top: 2px;
}
</style>
