<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- 用户卡 -->
    <div class="flex items-center justify-between flex-wrap gap-3 pb-4 border-b border-[hsl(var(--line))]">
      <div class="flex items-center gap-3">
        <a-avatar :size="64">{{ avatarText }}</a-avatar>
        <div>
          <div class="flex items-center gap-2">
            <span class="text-[18px] font-bold">{{ data.user.name || '--' }}</span>
            <a-tag color="blue">{{ data.user.roleName || '--' }}</a-tag>
          </div>
          <div class="text-[hsl(var(--secondary-text))] mt-1">
            {{ t('common.department') }}：{{ data.user.deptName || '--' }} · {{ t('profile.employeeNo') }}：{{ data.user.employeeNo || '--' }}
          </div>
        </div>
      </div>
    </div>

    <!-- 6 统计卡 -->
    <div class="stat-grid mt-4">
      <div v-for="s in statCards" :key="s.key" class="stat-card border border-[hsl(var(--line))] rounded">
        <div class="text-[22px] font-bold text-[hsl(var(--primary))]">{{ s.value }}</div>
        <div class="text-[hsl(var(--secondary-text))] mt-1">{{ s.label }}</div>
      </div>
    </div>

    <!-- Tabs -->
    <a-tabs v-model:activeKey="activeTab" class="mt-4">
      <!-- 订阅设置 -->
      <a-tab-pane key="subscription" :tab="t('profile.tab.subscription')">
        <a-alert type="info" show-icon class="mb-3" :message="t('profile.subscriptionTip')" />
        <div class="mb-2 font-semibold">{{ t('profile.currentSubscription') }}</div>
        <div class="flex flex-wrap gap-2">
          <a-tag v-for="c in data.subscriptions" :key="c" closable>{{ c }}</a-tag>
          <span v-if="!data.subscriptions.length" class="text-[hsl(var(--secondary-text))]">{{ t('common.noData') }}</span>
        </div>
      </a-tab-pane>

      <!-- 我的收藏 -->
      <a-tab-pane key="collect" :tab="t('profile.tab.collect')">
        <a-list :data-source="data.collects" item-layout="horizontal">
          <template #renderItem="{ item }">
            <a-list-item>
              <template #actions>
                <a>{{ t('profile.cancelCollect') }}</a>
              </template>
              <a-list-item-meta>
                <template #title>
                  <span class="flex items-center gap-2">
                    <span>{{ item.title }}</span>
                    <a-tag>{{ t('profile.itemType.' + item.type) }}</a-tag>
                    <a-tag v-if="item.isDeleted" color="red">{{ t('profile.deleted') }}</a-tag>
                  </span>
                </template>
                <template #description>
                  <span class="text-[hsl(var(--secondary-text))]">{{ item.createdAt }}</span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <!-- 我的发布 -->
      <a-tab-pane key="publish" :tab="t('profile.tab.publish')">
        <a-list :data-source="data.publishes" item-layout="horizontal">
          <template #renderItem="{ item }">
            <a-list-item>
              <template #actions>
                <a v-if="item.status === 'draft'">{{ t('profile.continueEdit') }}</a>
              </template>
              <a-list-item-meta>
                <template #title>
                  <span class="flex items-center gap-2">
                    <span>{{ item.title }}</span>
                    <a-tag :color="statusColor[item.status] ?? 'default'">{{ t('dict.oppStatus.' + item.status) }}</a-tag>
                  </span>
                </template>
                <template #description>
                  <span class="text-[hsl(var(--secondary-text))]">
                    {{ t('common.viewCount') }} {{ item.viewCount }} · {{ t('common.like') }} {{ item.likeCount }} ·
                    {{ t('common.comment') }} {{ item.commentCount }} · {{ item.createdAt }}
                  </span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <!-- 我的方案 -->
      <a-tab-pane key="solution" :tab="t('profile.tab.solution')">
        <a-list :data-source="data.solutions" item-layout="vertical">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <span class="flex items-center gap-2">
                    <a-tag color="orange">{{ t('profile.adopted') }}</a-tag>
                    <span>{{ item.title }}</span>
                  </span>
                </template>
                <template #description>
                  <span class="text-[hsl(var(--secondary-text))]">
                    {{ t('profile.relatedRequest') }}：{{ item.requestTitle }} · {{ t('profile.adoptedBy') }}：{{ item.adopterName }}（{{ item.adopterDeptName }}）
                  </span>
                </template>
              </a-list-item-meta>
              <div>{{ item.summary }}</div>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <!-- 我的评论 -->
      <a-tab-pane key="comment" :tab="t('profile.tab.comment')">
        <a-list :data-source="data.comments" item-layout="horizontal">
          <template #renderItem="{ item }">
            <a-list-item>
              <template #actions>
                <a>{{ t('profile.viewOrigin') }}</a>
              </template>
              <a-list-item-meta>
                <template #title>
                  <span>{{ item.content }}</span>
                </template>
                <template #description>
                  <span class="text-[hsl(var(--secondary-text))]">{{ item.sourceTitle }} · {{ item.createdAt }}</span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <!-- 浏览记录 -->
      <a-tab-pane key="history" :tab="t('profile.tab.history')">
        <a-list :data-source="data.viewHistory" item-layout="horizontal">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta>
                <template #title>
                  <span class="flex items-center gap-2">
                    <span>{{ item.title }}</span>
                    <a-tag>{{ t('profile.itemType.' + item.type) }}</a-tag>
                  </span>
                </template>
                <template #description>
                  <span class="text-[hsl(var(--secondary-text))]">{{ t('profile.viewedAt') }}：{{ item.viewedAt }}</span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getProfile } from '@/apis/profile/profileApi'
import type { ProfileAggregate } from '@/apis/profile/types'

defineOptions({ name: 'ProfileCenter' })
definePage({
  name: 'ProfileCenter',
  meta: {
    layout: 'default',
    order: 4,
    menu: { icon: 'q-icon:bookmark-linear' },
    title: 'profile'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()

const statusColor: Record<string, string> = { published: 'green', archived: 'red', draft: 'default' }

const data = reactive<ProfileAggregate>({
  user: { name: '', deptName: '', roleName: '', employeeNo: '' },
  stats: { collectCount: 0, commentCount: 0, publishCount: 0, solutionCount: 0, draftCount: 0, viewCount: 0 },
  subscriptions: [],
  collects: [],
  publishes: [],
  solutions: [],
  comments: [],
  viewHistory: []
})

const activeTab = ref('subscription')

const avatarText = computed(() => (data.user.name ? data.user.name.slice(0, 1) : '--'))

const statCards = computed(() => [
  { key: 'collect', label: t('profile.stats.collect'), value: data.stats.collectCount },
  { key: 'comment', label: t('profile.stats.comment'), value: data.stats.commentCount },
  { key: 'publish', label: t('profile.stats.publish'), value: data.stats.publishCount },
  { key: 'solution', label: t('profile.stats.solution'), value: data.stats.solutionCount },
  { key: 'draft', label: t('profile.stats.draft'), value: data.stats.draftCount },
  { key: 'view', label: t('profile.stats.view'), value: data.stats.viewCount }
])

onMounted(async () => {
  const res = await getProfile()
  Object.assign(data, res)
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}
.stat-card {
  padding: 16px;
  text-align: center;
}
@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
