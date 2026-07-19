<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- Hero 搜索区 -->
    <div class="hero rounded-[8px] px-[24px] py-[28px] mb-4">
      <h1 class="text-[22px] font-bold text-white mb-1">{{ t('home.heroTitle') }}</h1>
      <p class="text-white/80 mb-4">{{ t('home.heroSubtitle') }}</p>
      <a-input-search
        v-model:value="keyword"
        :placeholder="t('home.searchPlaceholder')"
        enter-button
        size="large"
        class="max-w-[560px]"
        @search="onSearch"
      />
      <div class="mt-3 flex items-center flex-wrap gap-2">
        <span class="text-white/70">{{ t('home.hotSearch') }}</span>
        <a-tag
          v-for="tag in dashboard?.hotTags ?? []"
          :key="tag"
          class="cursor-pointer"
          @click="onSearch(tag)"
          >{{ tag }}</a-tag
        >
      </div>
    </div>

    <!-- 4 统计卡 -->
    <a-row :gutter="16" class="mb-4">
      <a-col v-for="s in statCards" :key="s.key" :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="false" class="stat-card">
          <div class="text-[26px] font-bold text-[hsl(var(--primary))]">{{ s.value }}</div>
          <div class="text-[hsl(var(--secondary-text))] mt-1">{{ s.label }}</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 快捷操作 4 卡 -->
    <div class="text-[15px] font-semibold mb-2">{{ t('home.quickActions') }}</div>
    <a-row :gutter="16" class="mb-4">
      <a-col v-for="q in quickActions" :key="q.key" :xs="12" :sm="12" :md="6">
        <a-card hoverable size="small" class="mb-2" @click="router.push(q.path)">
          <div class="flex items-center gap-2">
            <span class="text-[22px]">{{ q.emoji }}</span>
            <div>
              <div class="font-medium">{{ q.title }}</div>
              <div class="text-[12px] text-[hsl(var(--secondary-text))]">{{ q.desc }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <!-- 我的任务看板 -->
      <a-col :xs="24" :md="14">
        <a-card size="small" class="mb-4">
          <template #title>
            <span>{{ t('home.taskBoard') }}</span>
            <a-tag color="blue" class="ml-2">{{ t('home.pending') }} {{ pendingCount }}</a-tag>
            <a-tag color="red">{{ t('home.overdue') }} {{ overdueCount }}</a-tag>
          </template>
          <template #extra>
            <a class="text-[hsl(var(--primary))]" @click="router.push('/task')">{{ t('home.viewAll') }}</a>
          </template>
          <a-list :data-source="dashboard?.quickTasks ?? []" :locale="{ emptyText: t('home.noTasks') }">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="w-full flex items-center justify-between gap-2">
                  <div class="flex items-center gap-2 min-w-0">
                    <span class="dot" :class="'dot-' + item.priority"></span>
                    <span class="truncate">{{ item.title }}</span>
                    <span class="text-[12px] text-[hsl(var(--secondary-text))] shrink-0">{{ item.meetingName }}</span>
                  </div>
                  <div class="flex items-center gap-1 shrink-0">
                    <span
                      class="text-[12px]"
                      :class="item.isOverdue ? 'text-[hsl(var(--error))]' : 'text-[hsl(var(--secondary-text))]'"
                      >{{ item.deadline }}</span
                    >
                    <a-tag :color="priorityColor[item.priority] ?? 'default'">{{ t('dict.urgency.' + item.priority) }}</a-tag>
                    <a-tag :color="taskStatusColor[item.status] ?? 'default'">{{ t('dict.taskStatus.' + item.status) }}</a-tag>
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <!-- 热门方案 -->
        <a-card size="small" class="mb-4">
          <template #title>{{ t('home.hotSolutions') }}</template>
          <template #extra>
            <a class="text-[hsl(var(--primary))]" @click="router.push('/opportunity')">{{ t('home.browseAll') }}</a>
          </template>
          <a-row :gutter="12">
            <a-col v-for="sol in dashboard?.hotSolutions ?? []" :key="sol.id" :xs="24" :sm="12">
              <div
                class="sol-item p-2 rounded cursor-pointer mb-2"
                @click="router.push({ path: '/opportunity/detail', query: { id: sol.id } })"
              >
                <div class="flex items-center gap-1 mb-1">
                  <span class="rank">#{{ sol.rank }}</span>
                  <a-tag :color="typeColor[sol.type] ?? 'default'">{{ t('dict.oppType.' + sol.type) }}</a-tag>
                  <a-tag v-if="sol.isSubscribed" color="green">{{ t('home.subscribed') }}</a-tag>
                </div>
                <div class="truncate font-medium">{{ sol.title }}</div>
                <div class="text-[12px] text-[hsl(var(--secondary-text))] mt-1">
                  {{ sol.publisherName }} · {{ t('common.viewCount') }} {{ sol.viewCount }}
                </div>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>

      <!-- 右栏：公告 + 讨论热帖 -->
      <a-col :xs="24" :md="10">
        <a-card size="small" class="mb-4">
          <template #title>{{ t('home.announcements') }}</template>
          <a-list :data-source="dashboard?.announcements ?? []">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="w-full min-w-0">
                  <div class="flex items-center gap-1 mb-1">
                    <a-tag v-if="item.isPinned" color="red">{{ t('opportunity.pinned') }}</a-tag>
                    <a-tag :color="announceColor[item.type] ?? 'default'">{{ t('dict.announceType.' + item.type) }}</a-tag>
                    <span class="truncate font-medium">{{ item.title }}</span>
                  </div>
                  <div class="text-[12px] text-[hsl(var(--secondary-text))]">
                    {{ item.publisherName }} · {{ item.publishedAt }} · {{ t('common.viewCount') }} {{ item.viewCount }}
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

        <a-card size="small" class="mb-4">
          <template #title>{{ t('home.hotPosts') }}</template>
          <template #extra>
            <a class="text-[hsl(var(--primary))]" @click="router.push('/discussion')">{{ t('home.viewAll') }}</a>
          </template>
          <a-list :data-source="dashboard?.hotPosts ?? []">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="w-full min-w-0">
                  <div class="flex items-center gap-1 mb-1">
                    <a-tag :color="topicColor[item.topic] ?? 'default'">{{ t('home.topic.' + item.topic) }}</a-tag>
                    <span class="truncate font-medium">{{ item.title }}</span>
                  </div>
                  <div class="text-[12px] text-[hsl(var(--secondary-text))]">
                    {{ item.authorName }} · {{ t('home.replies') }} {{ item.replyCount }} ·
                    {{ t('common.viewCount') }} {{ item.viewCount }} · {{ item.createdAt }}
                  </div>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getHomeDashboard } from '@/apis/home/homeApi'
import type { HomeDashboard } from '@/apis/home/types'

defineOptions({ name: 'HomeDashboard' })
definePage({
  name: 'HomeDashboard',
  meta: {
    layout: 'default',
    menu: { icon: 'q-icon:home-linear' },
    title: 'home'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const keyword = ref('')
const dashboard = ref<HomeDashboard | null>(null)

const priorityColor: Record<string, string> = { critical: 'red', urgent: 'orange', normal: 'default' }
const taskStatusColor: Record<string, string> = { pending: 'blue', processing: 'orange', done: 'green', transferred: 'default' }
const typeColor: Record<string, string> = { product_info: 'blue', solution: 'green', success_case: 'orange' }
const announceColor: Record<string, string> = { notice: 'blue', policy: 'default', activity: 'green', other: 'default' }
const topicColor: Record<string, string> = { opportunity: 'blue', solution: 'orange', experience: 'green', industry: 'default', complaint: 'red' }

const statCards = computed(() => [
  { key: 'solutionTotal', label: t('home.stats.solutionTotal'), value: dashboard.value?.stats.solutionTotal ?? 0 },
  { key: 'pendingRequests', label: t('home.stats.pendingRequests'), value: dashboard.value?.stats.pendingRequests ?? 0 },
  { key: 'weekDiscussions', label: t('home.stats.weekDiscussions'), value: dashboard.value?.stats.weekDiscussions ?? 0 },
  { key: 'activeUsers', label: t('home.stats.activeUsers'), value: dashboard.value?.stats.activeUsers ?? 0 }
])

const quickActions = computed(() => [
  { key: 'browse', emoji: '🔍', title: t('home.action.browse'), desc: t('home.action.browseDesc'), path: '/opportunity' },
  { key: 'postReq', emoji: '📝', title: t('home.action.postReq'), desc: t('home.action.postReqDesc'), path: '/requirement' },
  { key: 'publish', emoji: '📤', title: t('home.action.publish'), desc: t('home.action.publishDesc'), path: '/opportunity/form' },
  { key: 'discuss', emoji: '💬', title: t('home.action.discuss'), desc: t('home.action.discussDesc'), path: '/discussion' }
])

const pendingCount = computed(() => (dashboard.value?.quickTasks ?? []).filter((x) => x.status === 'pending').length)
const overdueCount = computed(() => (dashboard.value?.quickTasks ?? []).filter((x) => x.isOverdue).length)

function onSearch(value: string) {
  const kw = typeof value === 'string' ? value : keyword.value
  router.push({ path: '/opportunity', query: kw ? { keyword: kw } : {} })
}

onMounted(async () => {
  dashboard.value = await getHomeDashboard()
})
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, hsl(var(--primary)) 0%, hsl(var(--primary) / 0.7) 100%);
}

.stat-card {
  background: hsl(var(--card-bg));
}

.sol-item {
  background: hsl(var(--card-bg));
}
.sol-item:hover {
  background: hsl(var(--primary) / 0.06);
}

.rank {
  color: hsl(var(--primary));
  font-weight: 700;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 9999px;
  display: inline-block;
  flex-shrink: 0;
}
.dot-critical {
  background: hsl(var(--error));
}
.dot-urgent {
  background: #fa8c16;
}
.dot-normal {
  background: hsl(var(--secondary-text));
}
</style>
