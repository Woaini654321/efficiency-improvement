<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- ============ 详情态 ============ -->
    <template v-if="currentId">
      <div v-if="!detail" class="h-full flex items-center justify-center">
        <Empty type="noData" />
      </div>
      <div v-else>
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <a-tag v-if="detail.isPinned" color="red">{{ t('notification.pinned') }}</a-tag>
            <a-tag :color="typeColor[detail.type] ?? 'default'">{{ t('dict.announceType.' + detail.type) }}</a-tag>
            <a-tag v-if="detail.priority === 'important'" color="red">{{ t('dict.priority.important') }}</a-tag>
            <h2 class="text-[18px] font-bold">{{ detail.title }}</h2>
          </div>
          <a-button @click="backToList">{{ t('notification.backToList') }}</a-button>
        </div>

        <a-descriptions bordered size="small" :column="3">
          <a-descriptions-item :label="t('notification.publisher')">{{ detail.publisherName ?? '--' }}</a-descriptions-item>
          <a-descriptions-item :label="t('common.publishedAt')">{{ detail.publishedAt ?? '--' }}</a-descriptions-item>
          <a-descriptions-item :label="t('notification.viewCount')">{{ detail.viewCount }}</a-descriptions-item>
        </a-descriptions>

        <div class="mt-4">
          <div class="rich-body" v-html="detail.content"></div>
        </div>
      </div>
    </template>

    <!-- ============ 列表态 ============ -->
    <template v-else>
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-[18px] font-bold">{{ t('notification.announcementCenter') }}</h2>
      </div>
      <a-tabs v-model:activeKey="activeType" @change="loadList">
        <a-tab-pane v-for="tab in typeTabs" :key="tab.value" :tab="tab.label" />
      </a-tabs>

      <a-list :data-source="list" :loading="loading" item-layout="horizontal">
        <template #renderItem="{ item }">
          <a-list-item class="cursor-pointer" @click="goDetail(item.id)">
            <a-list-item-meta>
              <template #title>
                <span class="flex items-center gap-2">
                  <a-tag v-if="item.isPinned" color="red">{{ t('notification.pinned') }}</a-tag>
                  <a class="text-[hsl(var(--primary))]">{{ item.title }}</a>
                  <a-tag :color="typeColor[item.type] ?? 'default'">{{ t('dict.announceType.' + item.type) }}</a-tag>
                  <a-tag v-if="item.priority === 'important'" color="red">{{ t('dict.priority.important') }}</a-tag>
                </span>
              </template>
              <template #description>
                <span class="text-[hsl(var(--secondary-text))]">
                  {{ item.publisherName }} · {{ item.publishedAt }} · {{ t('notification.viewCount') }} {{ item.viewCount }}
                </span>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
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

// a-tag 颜色遵循 BUILD-PLAN §4 铁律：仅 blue/red/green/orange/default
const typeColor: Record<string, string> = { notice: 'blue', policy: 'orange', activity: 'green', other: 'default' }

const typeTabs = computed(() => [
  { label: t('common.all'), value: '' },
  { label: t('dict.announceType.notice'), value: 'notice' },
  { label: t('dict.announceType.policy'), value: 'policy' },
  { label: t('dict.announceType.activity'), value: 'activity' },
  { label: t('dict.announceType.other'), value: 'other' }
])

const currentId = computed(() => (route.query.id as string) || '')
const activeType = ref('')
const list = ref<AnnouncementItem[]>([])
const detail = ref<AnnouncementItem | null>(null)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await getAnnouncementList({ ...(activeType.value ? { type: activeType.value } : {}), pageNumber: 1, pageSize: 50 })
    list.value = res.records
  } finally {
    loading.value = false
  }
}

async function loadDetail(id: string) {
  detail.value = await getAnnouncementDetail(id)
}

function goDetail(id: string) {
  router.push({ path: '/notification/announcement', query: { id } })
}
function backToList() {
  router.push({ path: '/notification/announcement' })
}

watch(currentId, (id) => {
  if (id) loadDetail(id)
  else loadList()
})

onMounted(() => {
  if (currentId.value) loadDetail(currentId.value)
  else loadList()
})
</script>

<style scoped>
.rich-body {
  line-height: 1.8;
  color: hsl(var(--text));

  & h2 {
    font-size: 16px;
    font-weight: 600;
    margin: 12px 0 8px;
  }
}
</style>
