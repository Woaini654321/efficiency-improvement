<template>
  <div class="disc-browse">
    <!-- 标题区 -->
    <div class="page-head">
      <div>
        <div class="page-title">{{ t('discussion.browseTitle') }}</div>
        <div class="page-sub">{{ t('discussion.browseSubtitle') }}</div>
      </div>
      <a-button type="primary" @click="goPost">
        <template #icon><PlusOutlined /></template>
        {{ t('discussion.post') }}
      </a-button>
    </div>

    <!-- 话题分类胶囊 Tab -->
    <div class="topic-tabs">
      <button
        v-for="tp in topicTabs"
        :key="tp.value"
        class="topic-pill"
        :class="{ active: topicFilter === tp.value }"
        @click="topicFilter = tp.value"
      >
        {{ tp.label }} <span class="pill-count">{{ tp.count }}</span>
      </button>
    </div>

    <!-- 搜索 -->
    <div class="search-bar">
      <a-input-search
        v-model:value="keyword"
        :placeholder="t('discussion.searchPlaceholder')"
        allow-clear
        class="w-[400px] max-w-full"
      />
    </div>

    <!-- 双栏 -->
    <div class="layout-side">
      <!-- 左：帖子卡片列表 -->
      <a-spin :spinning="loading">
        <Empty v-if="!filtered.length" type="noData" :description="t('discussion.emptyHint')" />
        <div v-else class="post-list">
          <div
            v-for="item in filtered"
            :key="item.id"
            class="post-card"
            :class="{ vent: item.topic === 'complaint' }"
            @click="goDetail(item.id)"
          >
            <div class="post-head">
              <a-tag :color="topicColor[item.topic] || 'default'" class="topic-tag">
                {{ t('discussion.topic.' + item.topic) }}
              </a-tag>
              <a-badge v-if="item.isHot" :count="t('discussion.hot')" class="hot-badge" />
              <span class="post-title">{{ item.title }}</span>
            </div>
            <div class="post-summary">{{ item.content }}</div>
            <div class="post-meta">
              <span class="meta-author">
                <UserOutlined v-if="item.topic !== 'complaint'" />
                <span v-else class="ghost">👻</span>
                {{ item.authorName }}
              </span>
              <span><MessageOutlined /> {{ item.replyCount }} {{ t('discussion.replies') }}</span>
              <span><EyeOutlined /> {{ item.viewCount }} {{ t('discussion.views') }}</span>
              <span>{{ relativeTime(item.createdAt) }}</span>
              <span v-for="tg in item.tags.slice(0, 3)" :key="tg" class="tag-chip">{{ tg }}</span>
            </div>
          </div>
        </div>
      </a-spin>

      <!-- 右：侧栏 -->
      <div class="side-col">
        <div class="side-card">
          <h4><FireOutlined class="text-[hsl(var(--error))]" /> {{ t('discussion.hotDiscussion') }}</h4>
          <div
            v-for="p in hotPosts"
            :key="p.id"
            class="hot-item"
            @click="goDetail(p.id)"
          >
            <div class="hot-title">{{ p.title }}</div>
            <div class="hot-sub">{{ p.replyCount }} {{ t('discussion.replies') }} · {{ p.viewCount }} {{ t('discussion.views') }}</div>
          </div>
        </div>

        <div class="side-card">
          <h4><TagsOutlined class="text-[hsl(var(--primary))]" /> {{ t('discussion.hotTags') }}</h4>
          <div class="tag-cloud">
            <span
              v-for="tg in hotTags"
              :key="tg"
              class="cloud-tag"
              @click="keyword = tg"
            >{{ tg }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  PlusOutlined, UserOutlined, EyeOutlined, MessageOutlined, FireOutlined, TagsOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import { getDiscussionList } from '@/apis/discussion/discussionApi'
import type { DiscussionItem } from '@/apis/discussion/types'

defineOptions({ name: 'DiscussionList' })
definePage({
  name: 'DiscussionList',
  meta: {
    layout: false,
    menu: true,
    title: 'discussion.list'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const topicColor: Record<string, string> = {
  business: 'blue',
  solution: 'orange',
  experience: 'green',
  industry: 'purple',
  complaint: 'magenta'
}

const TOPIC_ORDER = ['business', 'solution', 'experience', 'industry', 'complaint']

const allItems = ref<DiscussionItem[]>([])
const loading = ref(false)
const keyword = ref('')
const topicFilter = ref('')

const topicTabs = computed(() => {
  const tabs = [{ label: t('common.all'), value: '', count: allItems.value.length }]
  TOPIC_ORDER.forEach((tp) => {
    tabs.push({
      label: t('discussion.topic.' + tp),
      value: tp,
      count: allItems.value.filter((i) => i.topic === tp).length
    })
  })
  return tabs
})

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let list = allItems.value
  if (topicFilter.value) list = list.filter((i) => i.topic === topicFilter.value)
  if (kw) {
    list = list.filter(
      (i) =>
        (i.title + i.content).toLowerCase().includes(kw) ||
        i.tags.some((tg) => tg.toLowerCase().includes(kw))
    )
  }
  return [...list].sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
})

// 热门讨论 Top5（按浏览量）
const hotPosts = computed(() =>
  [...allItems.value].sort((a, b) => b.viewCount - a.viewCount).slice(0, 5)
)

// 热门标签云（按出现频次 Top10）
const hotTags = computed(() => {
  const freq = new Map<string, number>()
  allItems.value.forEach((i) => i.tags.forEach((tg) => freq.set(tg, (freq.get(tg) || 0) + 1)))
  return [...freq.entries()].sort((a, b) => b[1] - a[1]).slice(0, 10).map((e) => e[0])
})

function relativeTime(dateStr: string): string {
  if (!dateStr) return '--'
  const time = new Date(dateStr.replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return dateStr
  const day = Math.floor((Date.now() - time) / 86400000)
  if (day < 1) return t('common.today')
  if (day < 30) return t('common.daysAgo', { n: day })
  return dateStr.slice(0, 10)
}

function goDetail(id: string) {
  router.push({ path: '/discussion/detail', query: { id } })
}
function goPost() {
  router.push({ path: '/discussion/post' })
}

async function load() {
  loading.value = true
  try {
    const res = await getDiscussionList({ pageNumber: 1, pageSize: 999 })
    allItems.value = res.records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.disc-browse {
  padding: 16px;
}
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: hsl(var(--text));
}
.page-sub {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  margin-top: 4px;
}
.topic-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.topic-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 16px;
  border: 1px solid hsl(var(--line));
  background: #fff;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  color: hsl(var(--text));
  transition: all 0.2s;
}
.topic-pill:hover {
  border-color: hsl(var(--primary) / 0.5);
  color: hsl(var(--primary));
}
.topic-pill.active {
  background: hsl(var(--primary));
  border-color: hsl(var(--primary));
  color: #fff;
}
.pill-count {
  font-size: 11px;
  opacity: 0.75;
}
.search-bar {
  margin-bottom: 18px;
}
.layout-side {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
}
.post-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.post-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 14px;
  padding: 18px 22px;
  cursor: pointer;
  transition: all 0.25s;
}
.post-card:hover {
  border-color: hsl(var(--primary));
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transform: translateX(3px);
}
.post-card.vent {
  border-left: 3px solid #ffadd2;
}
.post-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.topic-tag {
  border: none;
  border-radius: 6px;
}
.hot-badge :deep(.ant-badge-count) {
  background: hsl(var(--error));
  font-size: 10px;
  box-shadow: none;
}
.post-title {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--text));
}
.post-summary {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.post-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.meta-author {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.ghost {
  color: #eb2f96;
}
.tag-chip {
  background: hsl(var(--card-bg));
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 11px;
  color: hsl(var(--secondary-text));
}
.side-col {
  display: flex;
  flex-direction: column;
}
.side-card {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 14px;
  padding: 18px 20px;
  margin-bottom: 16px;
}
.side-card h4 {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.hot-item {
  padding: 8px 0;
  border-bottom: 1px solid hsl(var(--line) / 0.6);
  cursor: pointer;
}
.hot-item:last-child {
  border-bottom: none;
}
.hot-title {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--text));
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hot-title:hover {
  color: hsl(var(--primary));
}
.hot-sub {
  font-size: 11px;
  color: hsl(var(--secondary-text));
  margin-top: 2px;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cloud-tag {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.08);
  cursor: pointer;
  transition: background 0.2s;
}
.cloud-tag:hover {
  background: hsl(var(--primary) / 0.16);
}
@media (max-width: 768px) {
  .layout-side {
    grid-template-columns: 1fr;
  }
}
</style>
