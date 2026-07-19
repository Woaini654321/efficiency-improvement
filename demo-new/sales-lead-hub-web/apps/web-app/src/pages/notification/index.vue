<template>
  <div class="notify-page">
    <!-- 页头 -->
    <div class="notify-header">
      <div class="header-left">
        <div class="title-group">
          <BellOutlined class="bell" />
          <span class="page-title">{{ t('page.notification.list') }}</span>
          <a-badge :count="unreadCount" :overflow-count="99" />
        </div>
        <a-segmented v-model:value="readFilter" :options="segOptions" @change="onFilterChange" />
        <a-select
          class="w-[140px]"
          :value="typeFilter"
          :placeholder="t('notification.type')"
          allow-clear
          :options="typeSelectOptions"
          @update:value="(v: any) => { typeFilter = v || ''; onFilterChange() }"
        />
      </div>
      <div class="header-right">
        <span class="unread-only">
          <FilterOutlined />
          {{ t('notification.unreadOnly') }}
          <a-switch size="small" :checked="readFilter === 'unread'" @change="onUnreadOnly" />
        </span>
        <a-tooltip :title="t('notification.gotoPreferenceTip')">
          <span class="pref-link" @click="router.push('/notification/preference')">
            <SettingOutlined />
            {{ t('notification.gotoPreference') }}
          </span>
        </a-tooltip>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="notify-toolbar">
      <div class="toolbar-left">
        <a-input-search
          v-model:value="searchKey"
          :placeholder="t('notification.searchContentPlaceholder')"
          allow-clear
          size="small"
          class="w-[260px]"
          @search="onFilterChange"
        />
        <a-button size="small" :type="batchMode ? 'primary' : 'default'" @click="toggleBatch">
          <template #icon><CheckSquareOutlined /></template>
          {{ batchMode ? t('notification.exitBatch') : t('notification.batchMode') }}
        </a-button>
      </div>
      <div class="toolbar-right">
        <a-button size="small" @click="markAllRead">
          <template #icon><CheckOutlined /></template>
          {{ t('notification.markAllRead') }}
        </a-button>
        <a-button size="small" danger @click="clearRead">
          <template #icon><DeleteOutlined /></template>
          {{ t('notification.clearRead') }}
        </a-button>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div v-if="batchMode && selectedIds.length" class="batch-bar">
      <span class="batch-info">
        <CheckSquareOutlined />
        {{ t('notification.selectedCount', { n: selectedIds.length }) }}
      </span>
      <div class="flex gap-2">
        <a-button size="small" @click="markSelectedRead">
          <template #icon><EyeOutlined /></template>
          {{ t('notification.markRead') }}
        </a-button>
        <a-button size="small" danger @click="deleteSelected">
          <template #icon><DeleteOutlined /></template>
          {{ t('notification.deleteSelected') }}
        </a-button>
      </div>
    </div>

    <!-- 通知列表 -->
    <a-spin :spinning="loading">
      <div class="notify-list">
        <div v-if="paged.length" class="list-head">
          <span class="list-head-title"><UnorderedListOutlined /> {{ t('notification.listTitle') }}</span>
          <a-button type="text" size="small" class="text-primary" @click="markAllRead">
            <template #icon><CheckOutlined /></template>
            {{ t('notification.markAllReadShort') }}
          </a-button>
        </div>
        <div v-if="batchMode && paged.length" class="select-all-row">
          <a-checkbox :checked="allCurrentSelected" :indeterminate="someCurrentSelected" @change="selectAllCurrent">
            {{ t('notification.selectAll') }}
          </a-checkbox>
        </div>

        <div v-if="!paged.length" class="empty-state">
          <BellOutlined class="empty-icon" />
          <div>{{ emptyText }}</div>
        </div>

        <template v-else>
          <div v-for="group in activeGroups" :key="group" class="time-section">
            <div class="time-section-header">{{ t('notification.group.' + group) }}</div>
            <template v-for="item in groupItems(group)" :key="item.id">
              <div
                class="notify-row"
                :class="{ expanded: expandedId === item.id, selected: selectedIds.includes(item.id), 'show-check': batchMode }"
                @click="onRowClick(item)"
              >
                <div v-if="batchMode" class="row-select" @click.stop>
                  <a-checkbox :checked="selectedIds.includes(item.id)" @change="() => toggleSelect(item.id)" />
                </div>
                <span class="dot" :class="item.isRead ? 'read' : 'unread'"></span>
                <div class="type-icon" :class="metaOf(item.type).cls">
                  <component :is="metaOf(item.type).icon" />
                </div>
                <div class="notify-body">
                  <div class="notify-title">
                    {{ item.title }}
                    <span v-if="!item.isRead" class="title-dot"></span>
                  </div>
                  <div class="notify-desc">{{ item.triggerUserName }} · {{ relativeTime(item.createdAt) }}</div>
                </div>
                <div class="notify-right" @click.stop>
                  <a-tag v-if="item.isForceConfirm && !item.isRead" color="red">
                    <ExclamationCircleOutlined /> {{ t('notification.forceConfirm') }}
                  </a-tag>
                  <a-tag :color="metaOf(item.type).color">{{ t('dict.notifyType.' + item.type) }}</a-tag>
                  <a-button type="text" size="small" @click="toggleExpand(item)">
                    <template #icon>
                      <UpOutlined v-if="expandedId === item.id" />
                      <DownOutlined v-else />
                    </template>
                  </a-button>
                </div>
              </div>
              <div v-if="expandedId === item.id" class="expand-body">
                <div class="expand-card">
                  <div><strong>{{ t('notification.triggerUser') }}:</strong>{{ item.triggerUserName }}</div>
                  <div><strong>{{ t('notification.detailTime') }}:</strong>{{ item.createdAt }}</div>
                  <div><strong>{{ t('notification.detailLabel') }}:</strong>{{ item.title }}</div>
                  <div v-if="item.targetId" class="mt-2">
                    <a-button type="primary" size="small" ghost @click="goTarget(item)">
                      {{ t('notification.viewDetail') }} →
                    </a-button>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </template>
      </div>

      <div v-if="filtered.length > pageSize" class="pager">
        <a-pagination
          v-model:current="page"
          :total="filtered.length"
          :page-size="pageSize"
          size="small"
          :show-total="(tt: number) => t('common.totalItems', { n: tt })"
          @change="expandedId = null"
        />
      </div>
    </a-spin>

    <!-- 强制确认阅读弹窗 -->
    <a-modal
      :open="forceModalVisible"
      :title="undefined"
      :width="640"
      :mask-closable="false"
      :closable="false"
      :footer="null"
    >
      <template v-if="forceItem">
        <div class="force-title">
          <ExclamationCircleOutlined class="force-title-icon" />
          {{ t('notification.forceConfirmTitle') }}
        </div>
        <p class="force-intro">{{ t('notification.forceConfirmIntro') }}</p>
        <div class="force-card">
          <p><strong>{{ t('notification.forceConfirmSummary') }}:</strong>{{ forceItem.title }}</p>
          <p><strong>{{ t('notification.triggerUser') }}:</strong>{{ forceItem.triggerUserName }}</p>
          <p><strong>{{ t('notification.detailTime') }}:</strong>{{ forceItem.createdAt }}</p>
          <p class="force-note">{{ t('notification.forceConfirmNote') }}</p>
        </div>
        <a-checkbox v-model:checked="forceChecked">
          <span class="font-medium">{{ t('notification.forceConfirmCheck') }}</span>
        </a-checkbox>
        <div class="force-footer">
          <a-button type="primary" :disabled="!forceChecked" @click="confirmForce">
            <template #icon><CheckOutlined /></template>
            {{ t('notification.forceConfirmOk') }}
          </a-button>
        </div>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import type { Component } from 'vue'
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  BellOutlined, FilterOutlined, SettingOutlined, CheckSquareOutlined, CheckOutlined,
  DeleteOutlined, EyeOutlined, UnorderedListOutlined, UpOutlined, DownOutlined,
  ExclamationCircleOutlined, FileTextOutlined, MessageOutlined, TrophyOutlined,
  InfoCircleOutlined, CommentOutlined, SoundOutlined
} from '@ant-design/icons-vue'
import { getNotificationList, markNotificationRead } from '@/apis/notification/notificationApi'
import type { NotificationItem } from '@/apis/notification/types'

defineOptions({ name: 'NotificationList' })
definePage({
  name: 'NotificationList',
  meta: {
    layout: false,
    menu: true,
    title: 'notification.list'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

interface TypeMeta {
  color: string
  icon: Component
  cls: string
}
const typeMeta: Record<string, TypeMeta> = {
  publish: { color: 'blue', icon: FileTextOutlined, cls: 'ic-publish' },
  response: { color: 'green', icon: MessageOutlined, cls: 'ic-response' },
  adopt: { color: 'orange', icon: TrophyOutlined, cls: 'ic-adopt' },
  system: { color: 'default', icon: InfoCircleOutlined, cls: 'ic-system' },
  mention: { color: 'blue', icon: CommentOutlined, cls: 'ic-mention' },
  subscribe: { color: 'green', icon: SoundOutlined, cls: 'ic-subscribe' },
  force_confirm: { color: 'red', icon: ExclamationCircleOutlined, cls: 'ic-force' }
}
const FALLBACK_META: TypeMeta = { color: 'default', icon: InfoCircleOutlined, cls: 'ic-system' }
function metaOf(type: string): TypeMeta {
  return typeMeta[type] ?? FALLBACK_META
}

const TYPE_KEYS = ['publish', 'response', 'adopt', 'mention', 'subscribe', 'system', 'force_confirm'] as const

const items = ref<NotificationItem[]>([])
const loading = ref(false)
const readFilter = ref<string>('all')
const typeFilter = ref('')
const searchKey = ref('')
const page = ref(1)
const pageSize = 8
const expandedId = ref<string | null>(null)
const batchMode = ref(false)
const selectedIds = ref<string[]>([])

const forceModalVisible = ref(false)
const forceItem = ref<NotificationItem | null>(null)
const forceChecked = ref(false)

const unreadCount = computed(() => items.value.filter((i) => !i.isRead).length)
const totalCount = computed(() => items.value.length)

const segOptions = computed(() => [
  { label: `${t('common.all')} (${totalCount.value})`, value: 'all' },
  { label: `${t('notification.unread')} (${unreadCount.value})`, value: 'unread' },
  { label: t('notification.read'), value: 'read' }
])

const typeSelectOptions = computed(() =>
  TYPE_KEYS.map((k) => ({ label: t('dict.notifyType.' + k), value: k }))
)

const filtered = computed(() => {
  let list = [...items.value]
  if (readFilter.value === 'unread') list = list.filter((i) => !i.isRead)
  else if (readFilter.value === 'read') list = list.filter((i) => i.isRead)
  if (typeFilter.value) list = list.filter((i) => i.type === typeFilter.value)
  const kw = searchKey.value.trim().toLowerCase()
  if (kw) list = list.filter((i) => (i.title + i.triggerUserName).toLowerCase().includes(kw))
  list.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  return list
})

const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))

const GROUP_ORDER = ['today', 'yesterday', 'week', 'older'] as const
type Group = (typeof GROUP_ORDER)[number]

function dayDiff(dateStr: string): number {
  if (!dateStr) return 999
  const d = new Date(dateStr.replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return 999
  const d0 = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const now = new Date()
  const n0 = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  return Math.round((n0 - d0) / 86400000)
}
function groupOf(dateStr: string): Group {
  const diff = dayDiff(dateStr)
  if (diff <= 0) return 'today'
  if (diff === 1) return 'yesterday'
  if (diff < 7) return 'week'
  return 'older'
}
const activeGroups = computed(() => GROUP_ORDER.filter((g) => paged.value.some((i) => groupOf(i.createdAt) === g)))
function groupItems(g: Group): NotificationItem[] {
  return paged.value.filter((i) => groupOf(i.createdAt) === g)
}

function relativeTime(dateStr: string): string {
  if (!dateStr) return '--'
  const time = new Date(dateStr.replace(/-/g, '/')).getTime()
  if (Number.isNaN(time)) return dateStr
  const diffMin = Math.floor((Date.now() - time) / 60000)
  if (diffMin < 1) return t('common.justNow')
  if (diffMin < 60) return t('common.minutesAgo', { n: diffMin })
  const diffHour = Math.floor(diffMin / 60)
  if (diffHour < 24) return t('common.hoursAgo', { n: diffHour })
  const diffDay = Math.floor(diffHour / 24)
  if (diffDay < 7) return t('common.daysAgo', { n: diffDay })
  return dateStr.slice(0, 10)
}

const emptyText = computed(() => {
  if (searchKey.value) return t('notification.emptySearch')
  if (readFilter.value === 'unread') return t('notification.emptyUnread')
  return t('notification.emptyDefault')
})

const allCurrentSelected = computed(() => paged.value.length > 0 && paged.value.every((i) => selectedIds.value.includes(i.id)))
const someCurrentSelected = computed(() => paged.value.some((i) => selectedIds.value.includes(i.id)) && !allCurrentSelected.value)

function onFilterChange() {
  page.value = 1
  expandedId.value = null
}
function onUnreadOnly(checked: boolean | string | number) {
  readFilter.value = checked ? 'unread' : 'all'
  onFilterChange()
}
function toggleBatch() {
  batchMode.value = !batchMode.value
  if (!batchMode.value) selectedIds.value = []
}
function toggleSelect(id: string) {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((x) => x !== id)
    : [...selectedIds.value, id]
}
function selectAllCurrent() {
  const ids = paged.value.map((i) => i.id)
  if (allCurrentSelected.value) {
    selectedIds.value = selectedIds.value.filter((id) => !ids.includes(id))
  } else {
    selectedIds.value = [...new Set([...selectedIds.value, ...ids])]
  }
}

function toggleExpand(item: NotificationItem) {
  if (batchMode.value) return
  expandedId.value = expandedId.value === item.id ? null : item.id
  if (expandedId.value === item.id && !item.isRead && !item.isForceConfirm) item.isRead = true
}
function onRowClick(item: NotificationItem) {
  if (batchMode.value) return
  if (expandedId.value === item.id) {
    expandedId.value = null
    return
  }
  expandedId.value = item.id
  if (item.isForceConfirm && !item.isRead) {
    forceItem.value = item
    forceChecked.value = false
    forceModalVisible.value = true
    return
  }
  if (!item.isRead) item.isRead = true
}

async function markRead(item: NotificationItem) {
  item.isRead = true
  try {
    await markNotificationRead(item.id)
  } catch {
    /* mock 无后端，忽略 */
  }
}
function markAllRead() {
  const hasForced = items.value.some((i) => i.isForceConfirm && !i.isRead)
  items.value.forEach((i) => {
    if (!(i.isForceConfirm && !i.isRead)) i.isRead = true
  })
  message.success(hasForced ? t('notification.markAllReadExcept') : t('notification.markAllReadDone'))
}
function clearRead() {
  const readCount = items.value.filter((i) => i.isRead).length
  if (readCount === 0) {
    message.info(t('notification.noReadToClear'))
    return
  }
  Modal.confirm({
    title: t('notification.clearReadTitle'),
    content: t('notification.clearReadConfirm', { n: readCount }),
    okText: t('notification.clearRead'),
    cancelText: t('common.cancel'),
    okType: 'danger',
    onOk: () => {
      items.value = items.value.filter((i) => !i.isRead)
      selectedIds.value = []
      message.success(t('notification.clearedN', { n: readCount }))
    }
  })
}
function markSelectedRead() {
  if (!selectedIds.value.length) return
  const n = selectedIds.value.length
  items.value.forEach((i) => {
    if (selectedIds.value.includes(i.id)) i.isRead = true
  })
  message.success(t('notification.markedN', { n }))
  selectedIds.value = []
  batchMode.value = false
}
function deleteSelected() {
  if (!selectedIds.value.length) return
  const n = selectedIds.value.length
  items.value = items.value.filter((i) => !selectedIds.value.includes(i.id))
  message.success(t('notification.deletedN', { n }))
  selectedIds.value = []
  batchMode.value = false
}

function confirmForce() {
  if (forceItem.value) void markRead(forceItem.value)
  forceModalVisible.value = false
  forceItem.value = null
  message.success(t('notification.forceConfirmDone'))
}

function goTarget(item: NotificationItem) {
  if (item.targetType === 'announcement') {
    router.push({ path: '/notification/announcement', query: { id: item.targetId } })
  } else if (item.targetType === 'opportunity') {
    router.push({ path: '/opportunity/detail', query: { id: item.targetId } })
  } else if (item.targetType === 'requirement') {
    router.push({ path: '/requirement/detail', query: { id: item.targetId } })
  } else {
    message.info(item.title)
  }
}

watch(filtered, () => {
  const maxPage = Math.max(1, Math.ceil(filtered.value.length / pageSize))
  if (page.value > maxPage) page.value = maxPage
})

async function load() {
  loading.value = true
  try {
    const res = await getNotificationList({ pageNumber: 1, pageSize: 999 })
    items.value = res.records
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<style scoped>
.notify-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 16px;
}
.notify-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.title-group {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bell {
  font-size: 22px;
  color: hsl(var(--primary));
}
.page-title {
  font-size: 20px;
  font-weight: 700;
  color: hsl(var(--text));
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}
.unread-only {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
}
.pref-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 13px;
  color: hsl(var(--primary));
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}
.pref-link:hover {
  background: hsl(var(--primary) / 0.08);
}
.notify-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-radius: 12px 12px 0 0;
}
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 16px;
  background: hsl(var(--primary) / 0.06);
  border: 1px solid hsl(var(--primary) / 0.3);
  border-top: none;
}
.batch-info {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: hsl(var(--primary));
}
.notify-list {
  background: #fff;
  border: 1px solid hsl(var(--line));
  border-top: none;
  border-radius: 0 0 12px 12px;
  overflow: hidden;
}
.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-bottom: 1px solid hsl(var(--line));
}
.list-head-title {
  font-size: 13px;
  color: hsl(var(--secondary-text));
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.text-primary {
  color: hsl(var(--primary));
}
.select-all-row {
  padding: 8px 20px;
  border-bottom: 1px solid hsl(var(--line));
}
.time-section-header {
  padding: 14px 20px 8px;
  font-size: 13px;
  font-weight: 600;
  color: hsl(var(--secondary-text));
  display: flex;
  align-items: center;
}
.time-section-header::after {
  content: '';
  flex: 1;
  height: 1px;
  background: hsl(var(--line));
  margin-left: 12px;
}
.notify-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid hsl(var(--line) / 0.5);
  cursor: pointer;
  transition: background 0.15s;
}
.notify-row:hover {
  background: hsl(var(--primary) / 0.03);
}
.notify-row.expanded {
  background: hsl(var(--primary) / 0.05);
}
.notify-row.selected {
  background: hsl(var(--primary) / 0.06);
}
.row-select {
  padding-top: 3px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}
.dot.unread {
  background: hsl(var(--error));
  box-shadow: 0 0 0 3px hsl(var(--error) / 0.15);
}
.dot.read {
  background: hsl(var(--line));
}
.type-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}
.ic-publish {
  background: #e6f7ff;
  color: #1890ff;
}
.ic-response {
  background: #f6ffed;
  color: #52c41a;
}
.ic-adopt {
  background: #fff7e6;
  color: #fa8c16;
}
.ic-system {
  background: #f5f5f5;
  color: #8c8c8c;
}
.ic-mention {
  background: #f9f0ff;
  color: #722ed1;
}
.ic-subscribe {
  background: #e6fffb;
  color: #13c2c2;
}
.ic-force {
  background: #fff1f0;
  color: #ff4d4f;
}
.notify-body {
  flex: 1;
  min-width: 0;
}
.notify-title {
  font-size: 14px;
  color: hsl(var(--text));
  line-height: 1.5;
}
.title-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: hsl(var(--error));
  margin-left: 6px;
  vertical-align: middle;
}
.notify-desc {
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-top: 2px;
}
.notify-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.expand-body {
  padding: 0 20px 16px 76px;
  border-bottom: 1px solid hsl(var(--line) / 0.5);
}
.expand-card {
  background: hsl(var(--card-bg));
  border-radius: 8px;
  padding: 14px 16px;
  font-size: 13px;
  color: hsl(var(--secondary-text));
  line-height: 1.9;
}
.empty-state {
  padding: 64px 0;
  text-align: center;
  color: hsl(var(--secondary-text));
}
.empty-icon {
  font-size: 44px;
  opacity: 0.4;
  display: block;
  margin-bottom: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.force-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 700;
  color: hsl(var(--text));
  margin-bottom: 12px;
}
.force-title-icon {
  color: hsl(var(--error));
  font-size: 20px;
}
.force-intro {
  color: hsl(var(--secondary-text));
  margin-bottom: 12px;
}
.force-card {
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 16px;
  line-height: 1.9;
}
.force-note {
  color: hsl(var(--secondary-text));
  font-size: 12px;
  margin-top: 8px;
}
.force-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
