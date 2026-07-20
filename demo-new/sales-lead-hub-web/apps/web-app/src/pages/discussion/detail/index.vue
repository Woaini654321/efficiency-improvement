<template>
  <div v-if="!detail" class="h-full p-[16px] bg-white rounded flex items-center justify-center">
    <Empty type="noData" />
  </div>
  <div v-else class="disc-detail">
    <a class="nav-back" @click="router.back()">
      <ArrowLeftOutlined /> {{ t('discussion.backToList') }}
    </a>

    <!-- Hero 卡 -->
    <div class="hero-card" :class="'topic-' + (topicClass[detail.topic] || 'blue')">
      <div class="decoration"></div>
      <div class="decoration2"></div>
      <div class="hero-tags">
        <a-tag v-if="detail.isHot" color="red" class="border-none rounded-[6px]">{{ t('discussion.hot') }}</a-tag>
        <a-tag :color="topicColor[detail.topic] || 'default'" class="border-none rounded-[6px]">
          {{ t('discussion.topic.' + detail.topic) }}
        </a-tag>
      </div>
      <h1 class="hero-title">{{ detail.title }}</h1>
      <div class="hero-meta">
        <span>
          <UserOutlined v-if="detail.topic !== 'complaint'" />
          <span v-else class="ghost">👻</span>
          {{ detail.authorName || '--' }}
        </span>
        <span class="divider"></span>
        <span><CalendarOutlined /> {{ detail.createdAt || '--' }}</span>
        <span class="divider"></span>
        <span><EyeOutlined /> {{ detail.viewCount }} {{ t('discussion.views') }}</span>
        <span class="divider"></span>
        <span><MessageOutlined /> {{ totalReplies }} {{ t('discussion.replies') }}</span>
      </div>
    </div>

    <!-- 正文卡 -->
    <div class="content-card">
      <div class="post-body">{{ detail.content }}</div>
    </div>

    <!-- 评论区 -->
    <div class="comment-card">
      <h3 class="comment-title">
        <MessageOutlined class="text-[hsl(var(--primary))]" />
        {{ t('discussion.comments') }} ({{ totalReplies }})
      </h3>
      <div class="comment-divider"></div>
      <Empty v-if="!detail.comments.length" type="noData" />
      <CommentTree v-else :nodes="detail.comments" :depth="0" @reply="onReply" />
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
  ArrowLeftOutlined, UserOutlined, CalendarOutlined, EyeOutlined, MessageOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import CommentTree from '@/components/comment-tree/index.vue'
import { getDiscussionDetail, replyDiscussion } from '@/apis/discussion/discussionApi'
import type { DiscussionItem, CommentNode } from '@/apis/discussion/types'

defineOptions({ name: 'DiscussionDetail' })
definePage({
  name: 'DiscussionDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'discussion.detail'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<DiscussionItem | null>(null)

const topicColor: Record<string, string> = {
  business: 'blue',
  solution: 'orange',
  experience: 'green',
  industry: 'purple',
  complaint: 'magenta'
}
const topicClass: Record<string, string> = {
  business: 'blue',
  solution: 'orange',
  experience: 'green',
  industry: 'purple',
  complaint: 'pink'
}

// 递归统计评论树总数（本地新增回复后自动重算）
const countTree = (nodes: CommentNode[]): number =>
  nodes.reduce((sum, n) => sum + 1 + countTree(n.children), 0)

const totalReplies = computed(() => (detail.value ? countTree(detail.value.comments) : 0))

// 递归查找父节点并插入新回复
const insertReply = (nodes: CommentNode[], parentId: string, node: CommentNode): boolean => {
  for (const n of nodes) {
    if (n.id === parentId) {
      n.children.push(node)
      return true
    }
    if (insertReply(n.children, parentId, node)) return true
  }
  return false
}

const onReply = async ({ parentId, content }: { parentId: string; content: string }) => {
  if (!detail.value) return
  try {
    // 回帖持久化：后端返回带真实 id 的新节点，再插入评论树（复用 insertReply）
    const node = await replyDiscussion({ postId: id, parentId, content })
    if (insertReply(detail.value.comments, parentId, node)) {
      message.success(t('discussion.replySuccess'))
    }
  } catch {
    // 请求失败：拦截器已弹 message，不插入节点
  }
}

onMounted(async () => {
  detail.value = await getDiscussionDetail(id)
})
</script>

<style scoped>
.disc-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px;
}
.nav-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 14px;
  color: hsl(var(--secondary-text));
  cursor: pointer;
}
.nav-back:hover {
  color: hsl(var(--primary));
}

/* Hero 卡 */
.hero-card {
  position: relative;
  overflow: hidden;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 16px;
  padding: 32px 36px;
  margin-bottom: 22px;
}
.hero-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
}
.hero-card.topic-blue::before { background: linear-gradient(90deg, #1677ff, #69b1ff); }
.hero-card.topic-orange::before { background: linear-gradient(90deg, #fa8c16, #ffc069); }
.hero-card.topic-green::before { background: linear-gradient(90deg, #52c41a, #95de64); }
.hero-card.topic-purple::before { background: linear-gradient(90deg, #722ed1, #b37feb); }
.hero-card.topic-pink::before { background: linear-gradient(90deg, #eb2f96, #ff85c0); }
.decoration {
  position: absolute;
  top: -80px;
  right: -80px;
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: hsl(var(--primary) / 0.03);
  pointer-events: none;
}
.decoration2 {
  position: absolute;
  bottom: -60px;
  left: -60px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: hsl(var(--primary) / 0.03);
  pointer-events: none;
}
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
  position: relative;
}
.hero-title {
  font-size: 24px;
  font-weight: 700;
  color: hsl(var(--text));
  line-height: 1.45;
  margin-bottom: 18px;
  position: relative;
}
.hero-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 13px;
  color: hsl(var(--secondary-text));
  position: relative;
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
.ghost {
  color: #eb2f96;
}

/* 正文卡 */
.content-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 16px;
  padding: 28px 36px;
  margin-bottom: 22px;
}
.post-body {
  font-size: 15px;
  color: hsl(var(--text));
  line-height: 2;
  background: hsl(var(--card-bg));
  border-radius: 12px;
  padding: 22px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 评论区 */
.comment-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 16px;
  padding: 24px 36px;
}
.comment-title {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--text));
  display: flex;
  align-items: center;
  gap: 8px;
}
.comment-divider {
  height: 1px;
  background: hsl(var(--line));
  margin: 12px 0 8px;
}
@media (max-width: 768px) {
  .hero-card,
  .content-card,
  .comment-card {
    padding: 20px 18px;
  }
  .hero-title {
    font-size: 20px;
  }
}
</style>
