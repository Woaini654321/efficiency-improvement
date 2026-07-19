<template>
  <div class="ann-page">
    <!-- ============ 详情态 ============ -->
    <template v-if="currentId">
      <div v-if="!detail" class="empty-state"><Empty type="noData" /></div>
      <div v-else class="detail-panel">
        <div class="detail-header">
          <div class="detail-header-left">
            <div class="detail-title">{{ detail.title }}</div>
            <div class="detail-meta-row">
              <a-tag :color="metaOf(detail.type).color">{{ t('dict.announceType.' + detail.type) }}</a-tag>
              <a-tag v-if="detail.priority === 'high'" color="red">{{ t('dict.priority.high') }}</a-tag>
              <a-tag v-if="detail.isPinned" color="red">{{ t('notification.pinned') }}</a-tag>
              <span><UserOutlined /> {{ t('notification.publisher') }}:{{ detail.publisherName || '--' }}</span>
              <span><CalendarOutlined /> {{ t('common.publishedAt') }}:{{ detail.publishedAt || '--' }}</span>
              <span><EyeOutlined /> {{ detail.viewCount }} {{ t('notification.reads') }}</span>
            </div>
          </div>
          <a-button @click="backToList">{{ t('notification.backToList') }}</a-button>
        </div>
        <div class="detail-content rich-body" v-html="detail.content"></div>
        <div class="detail-footer">
          <a-button type="text" class="text-secondary" @click="backToList">
            <template #icon><ArrowLeftOutlined /></template>
            {{ t('notification.backToList') }}
          </a-button>
          <div class="footer-meta">
            <span>{{ t('notification.announcementNo') }}:{{ detail.id }}</span>
            <span>{{ t('common.publishedAt') }}:{{ detail.publishedAt || '--' }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 列表态 ============ -->
    <template v-else>
      <div class="list-header">
        <div class="page-title"><SoundOutlined /> {{ t('notification.announcementCenter') }}</div>
        <div class="type-tabs">
          <button
            v-for="tab in typeTabs"
            :key="tab.value"
            class="type-tab"
            :class="{ active: activeType === tab.value }"
            @click="activeType = tab.value"
          >
            <component :is="tab.icon" />
            {{ tab.label }}
            <span class="count">({{ typeCounts[tab.value] || 0 }})</span>
          </button>
        </div>
      </div>

      <a-spin :spinning="loading">
        <div v-if="!filteredList.length" class="empty-state"><Empty type="noData" /></div>
        <div v-else class="ann-list">
          <div
            v-for="ann in filteredList"
            :key="ann.id"
            class="ann-card"
            @click="goDetail(ann.id)"
          >
            <div class="icon-wrap" :class="metaOf(ann.type).cls">
              <component :is="metaOf(ann.type).icon" />
            </div>
            <div class="card-body">
              <div class="card-title" :class="{ unread: !readIds.includes(ann.id) }">
                <span v-if="!readIds.includes(ann.id)" class="unread-dot"></span>
                {{ ann.title }}
              </div>
              <div class="card-meta">
                <a-tag :color="metaOf(ann.type).color">{{ t('dict.announceType.' + ann.type) }}</a-tag>
                <a-tag v-if="ann.priority === 'high'" color="red">{{ t('dict.priority.high') }}</a-tag>
                <a-tag v-if="ann.isPinned" color="red">{{ t('notification.pinned') }}</a-tag>
                <span><UserOutlined /> {{ ann.publisherName }}</span>
                <span><ClockCircleOutlined /> {{ ann.publishedAt }}</span>
                <span><EyeOutlined /> {{ ann.viewCount }} {{ t('notification.reads') }}</span>
              </div>
            </div>
            <RightOutlined class="card-arrow" />
          </div>
        </div>
      </a-spin>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import type { Component } from 'vue'
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  SoundOutlined, UserOutlined, ClockCircleOutlined, CalendarOutlined, EyeOutlined,
  RightOutlined, ArrowLeftOutlined, InfoCircleOutlined, FileTextOutlined,
  CalendarOutlined as ActivityIcon, EllipsisOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import { getAnnouncementList, getAnnouncementDetail } from '@/apis/notification/announcementApi'
import type { AnnouncementItem } from '@/apis/notification/types'

defineOptions({ name: 'NotificationAnnouncement' })
definePage({
  name: 'NotificationAnnouncement',
  meta: {
    layout: false,
    menu: false,
    title: 'notification.announcement'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

interface TypeMeta {
  color: string
  icon: Component
  cls: string
}
const typeMeta: Record<string, TypeMeta> = {
  notice: { color: 'blue', icon: InfoCircleOutlined, cls: 'ann-notice' },
  policy: { color: 'orange', icon: FileTextOutlined, cls: 'ann-policy' },
  activity: { color: 'green', icon: ActivityIcon, cls: 'ann-activity' },
  other: { color: 'default', icon: EllipsisOutlined, cls: 'ann-other' }
}
const FALLBACK_META: TypeMeta = { color: 'default', icon: EllipsisOutlined, cls: 'ann-other' }
function metaOf(type: string): TypeMeta {
  return typeMeta[type] ?? FALLBACK_META
}

const TAB_KEYS = ['notice', 'policy', 'activity', 'other'] as const

const typeTabs = computed(() => [
  { label: t('common.all'), value: '', icon: SoundOutlined as Component },
  ...TAB_KEYS.map((k) => ({ label: t('dict.announceType.' + k), value: k, icon: metaOf(k).icon }))
])

const currentId = computed(() => (route.query.id as string) || '')
const activeType = ref('')
const list = ref<AnnouncementItem[]>([])
const detail = ref<AnnouncementItem | null>(null)
const loading = ref(false)
const readIds = ref<string[]>([])

const publishedList = computed(() => list.value.filter((a) => a.status === 'published'))
const filteredList = computed(() =>
  activeType.value ? publishedList.value.filter((a) => a.type === activeType.value) : publishedList.value
)
const typeCounts = computed<Record<string, number>>(() => {
  const counts: Record<string, number> = { '': publishedList.value.length }
  publishedList.value.forEach((a) => {
    counts[a.type] = (counts[a.type] || 0) + 1
  })
  return counts
})

async function loadList() {
  loading.value = true
  try {
    const res = await getAnnouncementList({ pageNumber: 1, pageSize: 999 })
    list.value = res.records
  } finally {
    loading.value = false
  }
}
async function loadDetail(id: string) {
  detail.value = await getAnnouncementDetail(id)
  if (!readIds.value.includes(id)) readIds.value = [...readIds.value, id]
}
function goDetail(id: string) {
  if (!readIds.value.includes(id)) readIds.value = [...readIds.value, id]
  router.push({ path: '/notification/announcement', query: { id } })
}
function backToList() {
  router.push({ path: '/notification/announcement' })
}

watch(currentId, (id) => {
  if (id) loadDetail(id)
  else if (!list.value.length) loadList()
})

onMounted(() => {
  loadList()
  if (currentId.value) loadDetail(currentId.value)
})
</script>

<style scoped>
.ann-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
}
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: hsl(var(--text));
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-title :deep(svg) {
  color: hsl(var(--primary));
}
.type-tabs {
  display: flex;
  gap: 4px;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 10px;
  padding: 3px;
}
.type-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: hsl(var(--secondary-text));
  border-radius: 8px;
  transition: all 0.2s;
}
.type-tab:hover {
  color: hsl(var(--primary));
  background: hsl(var(--primary) / 0.06);
}
.type-tab.active {
  color: #fff;
  background: hsl(var(--primary));
  font-weight: 600;
}
.type-tab .count {
  font-size: 11px;
  opacity: 0.75;
}
.ann-list {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  overflow: hidden;
}
.ann-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid hsl(var(--line));
  cursor: pointer;
  transition: background 0.2s;
  border-left: 3px solid transparent;
}
.ann-card:last-child {
  border-bottom: none;
}
.ann-card:hover {
  background: hsl(var(--primary) / 0.03);
  border-left-color: hsl(var(--primary));
}
.icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 20px;
}
.ann-notice {
  background: linear-gradient(135deg, #e6f7ff, #bae7ff);
  color: #1890ff;
}
.ann-policy {
  background: linear-gradient(135deg, #fff7e6, #ffd591);
  color: #fa8c16;
}
.ann-activity {
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  color: #52c41a;
}
.ann-other {
  background: linear-gradient(135deg, #f5f5f5, #e8e8e8);
  color: #8c8c8c;
}
.card-body {
  flex: 1;
  min-width: 0;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--text));
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-title.unread {
  font-weight: 700;
}
.unread-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: hsl(var(--error));
  margin-right: 8px;
  vertical-align: middle;
}
.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: hsl(var(--secondary-text));
}
.card-arrow {
  color: hsl(var(--secondary-text));
  align-self: center;
  flex-shrink: 0;
}
.detail-panel {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  overflow: hidden;
}
.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  border-bottom: 1px solid hsl(var(--line));
}
.detail-header-left {
  flex: 1;
  min-width: 0;
}
.detail-title {
  font-size: 20px;
  font-weight: 700;
  color: hsl(var(--text));
  line-height: 1.4;
  margin-bottom: 12px;
}
.detail-meta-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.detail-meta-row span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.detail-content {
  padding: 28px;
  font-size: 14px;
  color: hsl(var(--text));
  line-height: 1.9;
}
.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 28px;
  border-top: 1px solid hsl(var(--line));
}
.text-secondary {
  color: hsl(var(--secondary-text));
}
.footer-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.empty-state {
  padding: 60px 0;
  display: flex;
  justify-content: center;
}

/* 富文本正文样式 */
.rich-body :deep(h2) {
  font-size: 18px;
  font-weight: 700;
  color: hsl(var(--text));
  margin: 24px 0 10px;
  padding-bottom: 6px;
  border-bottom: 2px solid hsl(var(--line));
}
.rich-body :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--text));
  margin: 20px 0 8px;
}
.rich-body :deep(p) {
  margin-bottom: 12px;
}
.rich-body :deep(ul),
.rich-body :deep(ol) {
  padding-left: 20px;
  margin-bottom: 12px;
}
.rich-body :deep(li) {
  margin-bottom: 4px;
}
.rich-body :deep(blockquote) {
  border-left: 3px solid hsl(var(--primary));
  padding: 10px 16px;
  margin: 12px 0;
  background: hsl(var(--primary) / 0.06);
  border-radius: 0 8px 8px 0;
  color: hsl(var(--secondary-text));
}
</style>
