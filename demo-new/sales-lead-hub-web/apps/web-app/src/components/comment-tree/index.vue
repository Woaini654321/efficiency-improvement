<template>
  <div :class="indentClass">
    <div v-for="node in nodes" :key="node.id" class="py-2">
      <!-- 头部：头像 + 作者 + 相对时间 -->
      <div class="flex items-center gap-2 mb-1">
        <a-avatar :size="avatarSize" class="bg-[hsl(var(--primary))] shrink-0">
          {{ firstChar(node.authorName) }}
        </a-avatar>
        <span class="font-medium text-[hsl(var(--text))]">{{ node.authorName }}</span>
        <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ relativeTime(node.createdAt) }}</span>
      </div>

      <!-- 正文 -->
      <div class="text-[hsl(var(--text))] leading-6 whitespace-pre-wrap">{{ node.content }}</div>

      <!-- 操作行 -->
      <div class="flex items-center gap-3 mt-1">
        <a-button type="link" size="small" class="px-0" @click="toggleReply(node.id)">
          {{ t('discussion.reply') }}
        </a-button>
        <a-button
          v-if="node.children.length"
          type="link"
          size="small"
          class="px-0"
          @click="toggleCollapse(node.id)"
        >
          {{
            collapsed[node.id]
              ? t('discussion.expandN', { n: countDescendants(node) })
              : t('discussion.collapseN', { n: countDescendants(node) })
          }}
        </a-button>
      </div>

      <!-- 内联回复输入框 -->
      <div v-if="replyOpenId === node.id" class="mt-2 mb-1">
        <a-textarea
          v-model:value="replyText"
          :placeholder="t('discussion.replyPlaceholder')"
          :rows="2"
          :maxlength="300"
        />
        <div class="mt-2 flex justify-end">
          <a-button
            type="primary"
            size="small"
            :disabled="!replyText.trim()"
            @click="submitReply(node.id)"
          >
            {{ t('discussion.publishReply') }}
          </a-button>
        </div>
      </div>

      <!-- 递归子评论 -->
      <CommentTree
        v-if="node.children.length && !collapsed[node.id]"
        :nodes="node.children"
        :depth="depth + 1"
        @reply="forwardReply"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CommentNode } from '@/apis/discussion/types'

defineOptions({ name: 'CommentTree' })

const props = withDefaults(
  defineProps<{
    nodes: CommentNode[]
    depth?: number
  }>(),
  { depth: 0 }
)

const emit = defineEmits<{
  reply: [payload: { parentId: string; content: string }]
}>()

const { t, locale } = useI18n()

// 头像逐级缩小，最小 20px
const avatarSize = computed(() => Math.max(32 - props.depth * 4, 20))

// 缩进随层级递增，超过上限则拉平（不再继续缩进）
const indentClass = computed(() => {
  if (props.depth === 0) return ''
  if (props.depth > 4) return ''
  return 'pl-4 border-l border-[hsl(var(--line))]'
})

const firstChar = (name: string) => (name ? Array.from(name)[0] : '')

// 相对时间：使用 Intl 本地化，避免硬编码文案
const relativeTime = (raw: string): string => {
  const ts = Date.parse((raw || '').replace(' ', 'T'))
  if (Number.isNaN(ts)) return raw
  const rtf = new Intl.RelativeTimeFormat(locale.value, { numeric: 'auto' })
  const diffSec = Math.round((ts - Date.now()) / 1000)
  const abs = Math.abs(diffSec)
  if (abs < 60) return rtf.format(diffSec, 'second')
  if (abs < 3600) return rtf.format(Math.round(diffSec / 60), 'minute')
  if (abs < 86400) return rtf.format(Math.round(diffSec / 3600), 'hour')
  if (abs < 2592000) return rtf.format(Math.round(diffSec / 86400), 'day')
  if (abs < 31536000) return rtf.format(Math.round(diffSec / 2592000), 'month')
  return rtf.format(Math.round(diffSec / 31536000), 'year')
}

// 后代评论总数
const countDescendants = (node: CommentNode): number =>
  node.children.reduce((sum, child) => sum + 1 + countDescendants(child), 0)

// 折叠状态（按节点 id）
const collapsed = reactive<Record<string, boolean>>({})
const toggleCollapse = (id: string) => {
  collapsed[id] = !collapsed[id]
}

// 回复输入状态（同层同一时刻仅展开一个）
const replyOpenId = ref<string | null>(null)
const replyText = ref('')
const toggleReply = (id: string) => {
  replyOpenId.value = replyOpenId.value === id ? null : id
  replyText.value = ''
}
const submitReply = (parentId: string) => {
  const content = replyText.value.trim()
  if (!content) return
  emit('reply', { parentId, content })
  replyOpenId.value = null
  replyText.value = ''
}

// 冒泡子层级的 reply 事件
const forwardReply = (payload: { parentId: string; content: string }) => emit('reply', payload)
</script>
