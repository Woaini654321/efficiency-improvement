<template>
  <div v-if="!detail" class="h-full p-[16px] bg-white rounded flex items-center justify-center">
    <Empty type="noData" />
  </div>
  <div v-else class="h-full p-[16px] bg-white rounded overflow-auto">
    <div class="flex items-center justify-between mb-4">
      <a-button @click="router.back()">{{ t('common.back') }}</a-button>
    </div>

    <!-- Hero 卡 -->
    <div class="p-4 rounded border border-[hsl(var(--line))] mb-4">
      <div class="flex items-center gap-2 mb-2">
        <a-tag v-if="detail.isHot" color="red">{{ t('discussion.hot') }}</a-tag>
        <a-tag :color="topicColor[detail.topic] ?? 'default'">{{ t('discussion.topic.' + detail.topic) }}</a-tag>
        <h2 class="text-[18px] font-bold">{{ detail.title }}</h2>
      </div>
      <a-descriptions size="small" :column="3">
        <a-descriptions-item :label="t('discussion.author')">{{ detail.authorName ?? '--' }}</a-descriptions-item>
        <a-descriptions-item :label="t('discussion.views')">{{ detail.viewCount }}</a-descriptions-item>
        <a-descriptions-item :label="t('discussion.replies')">{{ detail.replyCount }}</a-descriptions-item>
        <a-descriptions-item :label="t('common.createdAt')" :span="3">{{ detail.createdAt ?? '--' }}</a-descriptions-item>
      </a-descriptions>
    </div>

    <!-- 正文 -->
    <div class="mb-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('discussion.content') }}</h3>
      <div class="leading-7 text-[hsl(var(--text))] whitespace-pre-wrap">{{ detail.content }}</div>
    </div>

    <!-- 评论树 -->
    <div class="mt-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('discussion.comments') }}（{{ detail.replyCount }}）</h3>
      <Empty v-if="!detail.comments.length" type="noData" />
      <CommentTree v-else :nodes="detail.comments" :depth="0" />
    </div>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, onMounted, defineComponent } from 'vue'
import type { PropType } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Empty from '@q-web-plugin/empty'
import { getDiscussionDetail } from '@/apis/discussion/discussionApi'
import type { DiscussionItem, CommentItem } from '@/apis/discussion/types'

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
  industry: 'default',
  complaint: 'red'
}

// 递归评论组件（逐级缩进）
const CommentTree = defineComponent({
  name: 'CommentTree',
  props: {
    nodes: { type: Array as PropType<CommentItem[]>, required: true },
    depth: { type: Number, default: 0 }
  },
  setup(props) {
    return () => (
      <div class={props.depth > 0 ? 'pl-4 border-l border-[hsl(var(--line))]' : ''}>
        {props.nodes.map((c) => (
          <div key={c.id} class="py-2">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-medium text-[hsl(var(--text))]">{c.authorName}</span>
              <span class="text-[12px] text-[hsl(var(--secondary-text))]">{c.createdAt}</span>
            </div>
            <div class="text-[hsl(var(--text))] leading-6">{c.content}</div>
            {c.children.length ? <CommentTree nodes={c.children} depth={props.depth + 1} /> : null}
          </div>
        ))}
      </div>
    )
  }
})

onMounted(async () => {
  detail.value = await getDiscussionDetail(id)
})
</script>
