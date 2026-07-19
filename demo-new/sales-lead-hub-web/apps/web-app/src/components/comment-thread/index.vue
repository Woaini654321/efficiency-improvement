<template>
  <div class="comment-thread">
    <!-- 发表评论 -->
    <div class="post-box mb-4">
      <a-textarea
        v-model:value="draft"
        :rows="3"
        :maxlength="500"
        :placeholder="t('comment.placeholder')"
        allow-clear
      />
      <div class="flex justify-end mt-2">
        <a-button type="primary" :loading="!!loading" @click="submitTop">{{ t('comment.publish') }}</a-button>
      </div>
    </div>

    <a-spin :spinning="!!loading">
      <!-- 空状态 -->
      <Empty v-if="!comments.length" type="noData" />

      <!-- 评论列表 -->
      <div v-else class="flex flex-col gap-4">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="flex gap-3">
            <a-avatar class="avatar">{{ firstChar(c.authorName) }}</a-avatar>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="author">{{ c.authorName }}</span>
                <a-tag v-if="c.authorDept">{{ c.authorDept }}</a-tag>
                <span class="time">{{ relativeTime(c.createdAt) }}</span>
              </div>
              <div class="content mt-1">{{ c.content }}</div>
              <div class="flex items-center gap-4 mt-2">
                <span class="op" @click="emit('like', c.id)">
                  <LikeOutlined /> {{ t('comment.like') }} {{ c.likeCount }}
                </span>
                <span class="op" @click="toggleReply(c.id)">
                  <MessageOutlined /> {{ t('comment.reply') }}
                </span>
              </div>

              <!-- 回复输入框 -->
              <div v-if="replyingId === c.id" class="reply-box mt-2">
                <a-textarea
                  v-model:value="replyDraft"
                  :rows="2"
                  :maxlength="500"
                  :placeholder="t('comment.replyPlaceholder')"
                  allow-clear
                />
                <div class="flex justify-end gap-2 mt-2">
                  <a-button size="small" @click="cancelReply">{{ t('comment.cancel') }}</a-button>
                  <a-button size="small" type="primary" @click="submitReply(c.id)">{{ t('comment.publish') }}</a-button>
                </div>
              </div>

              <!-- 回复列表（二级，扁平缩进） -->
              <div v-if="c.replies.length" class="reply-list mt-2 flex flex-col gap-3">
                <div v-for="r in c.replies" :key="r.id" class="flex gap-2">
                  <a-avatar size="small" class="avatar">{{ firstChar(r.authorName) }}</a-avatar>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 flex-wrap">
                      <span class="author">{{ r.authorName }}</span>
                      <a-tag v-if="r.authorDept">{{ r.authorDept }}</a-tag>
                      <span class="time">{{ relativeTime(r.createdAt) }}</span>
                    </div>
                    <div class="content mt-1">{{ r.content }}</div>
                    <div class="flex items-center gap-4 mt-2">
                      <span class="op" @click="emit('like', r.id)">
                        <LikeOutlined /> {{ t('comment.like') }} {{ r.likeCount }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { LikeOutlined, MessageOutlined } from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import type { Comment } from '@/apis/interaction/types'

defineProps<{
  comments: Comment[]
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [payload: { content: string; parentId?: string }]
  like: [id: string]
}>()

const { t } = useI18n()

const draft = ref('')
const replyDraft = ref('')
const replyingId = ref<string>('')

function firstChar(name: string): string {
  return name ? name.charAt(0) : '?'
}

function relativeTime(dateStr: string): string {
  if (!dateStr) return '--'
  const time = new Date(dateStr.replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return dateStr
  const diff = Date.now() - time
  const min = Math.floor(diff / 60000)
  if (min < 1) return t('comment.justNow')
  if (min < 60) return t('comment.minutesAgo', { n: min })
  const hour = Math.floor(min / 60)
  if (hour < 24) return t('comment.hoursAgo', { n: hour })
  const day = Math.floor(hour / 24)
  if (day < 30) return t('comment.daysAgo', { n: day })
  return dateStr
}

function submitTop() {
  const content = draft.value.trim()
  if (!content) return
  emit('submit', { content })
  draft.value = ''
}

function toggleReply(id: string) {
  replyingId.value = replyingId.value === id ? '' : id
  replyDraft.value = ''
}

function cancelReply() {
  replyingId.value = ''
  replyDraft.value = ''
}

function submitReply(parentId: string) {
  const content = replyDraft.value.trim()
  if (!content) return
  emit('submit', { content, parentId })
  cancelReply()
}
</script>

<style scoped>
.post-box {
  padding: 12px;
  background: hsl(var(--card-bg));
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
}

.comment-item {
  padding-bottom: 16px;
  border-bottom: 1px solid hsl(var(--line));
}

.avatar {
  flex-shrink: 0;
  background: hsl(var(--primary));
  color: #fff;
}

.author {
  font-weight: 600;
  color: hsl(var(--text));
}

.time {
  font-size: 12px;
  color: hsl(var(--secondary-text));
}

.content {
  color: hsl(var(--text));
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.op {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
  cursor: pointer;

  &:hover {
    color: hsl(var(--primary));
  }
}

.reply-list {
  padding: 10px 12px;
  background: hsl(var(--card-bg));
  border-radius: 8px;
}
</style>
