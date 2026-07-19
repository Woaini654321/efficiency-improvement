<template>
  <div v-if="!detail" class="h-full p-[16px] bg-white rounded flex items-center justify-center">
    <Empty type="noData" />
  </div>
  <div v-else class="h-full p-[16px] bg-white rounded overflow-auto">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <a-tag v-if="detail.isPinned" color="red">{{ t('requirement.pinned') }}</a-tag>
        <a-tag :color="urgencyColor[detail.urgency] ?? 'default'">{{ t('dict.urgency.' + detail.urgency) }}</a-tag>
        <a-tag :color="statusColor[detail.status] ?? 'default'">{{ t('dict.reqStatus.' + detail.status) }}</a-tag>
        <h2 class="text-[18px] font-bold">{{ detail.title }}</h2>
      </div>
      <div class="flex gap-2">
        <a-button @click="goEdit">{{ t('common.edit') }}</a-button>
        <a-button @click="router.back()">{{ t('common.back') }}</a-button>
      </div>
    </div>

    <h3 class="text-[15px] font-semibold mb-2">{{ t('requirement.baseInfo') }}</h3>
    <a-descriptions bordered size="small" :column="2">
      <a-descriptions-item :label="t('requirement.publisher')">{{ detail.publisherName ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('requirement.dept')">{{ detail.publisherDeptName ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('requirement.industry')">{{ detail.industry ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('requirement.slaStatus')">
        <a-tag :color="slaColor[detail.slaStatus] ?? 'default'">{{ t('dict.slaStatus.' + detail.slaStatus) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item :label="t('requirement.visibility')">
        <a-tag :color="visibilityColor[detail.visibilityType] ?? 'default'">{{ t('requirement.visibilityType.' + detail.visibilityType) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item :label="t('common.createdAt')">{{ detail.createdAt ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('requirement.category')" :span="2">
        <template v-if="detail.categoryNames.length">
          <a-tag v-for="c in detail.categoryNames" :key="c">{{ c }}</a-tag>
        </template>
        <span v-else>--</span>
      </a-descriptions-item>
      <a-descriptions-item :label="t('requirement.metrics')" :span="2">
        {{ t('common.viewCount') }} {{ detail.viewCount }} · {{ t('requirement.responseCount') }} {{ detail.responseCount }}
      </a-descriptions-item>
    </a-descriptions>

    <div class="mt-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('requirement.description') }}</h3>
      <!-- mock 富文本内容展示（后端接入后由富文本字段渲染） -->
      <div class="rich-body" v-html="detail.description"></div>
    </div>

    <div class="mt-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('requirement.invitedProductLines') }}</h3>
      <template v-if="detail.invitedProductLineNames.length">
        <a-tag v-for="p in detail.invitedProductLineNames" :key="p" color="blue">{{ p }}</a-tag>
      </template>
      <Empty v-else type="noData" />
    </div>

    <div class="mt-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('requirement.responses') }}（{{ detail.responses.length }}）</h3>
      <div v-if="detail.responses.length" class="flex flex-col gap-3">
        <div
          v-for="r in detail.responses"
          :key="r.id"
          class="border border-[hsl(var(--line))] rounded p-3"
        >
          <div class="flex items-center gap-2 mb-1">
            <span class="font-semibold">{{ r.responderName }}</span>
            <a-tag>{{ r.responderDeptName }}</a-tag>
            <a-tag v-if="r.isAdopted" color="green">{{ t('requirement.adopted') }}</a-tag>
            <span class="text-[hsl(var(--secondary-text))] text-[12px] ml-auto">{{ r.createdAt }}</span>
          </div>
          <div class="text-[hsl(var(--text))]">{{ r.content }}</div>
        </div>
      </div>
      <Empty v-else type="noData" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Empty from '@q-web-plugin/empty'
import { getRequirementDetail } from '@/apis/requirement/requirementApi'
import type { RequirementItem } from '@/apis/requirement/types'

defineOptions({ name: 'RequirementDetail' })
definePage({
  name: 'RequirementDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'requirement.detail'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<RequirementItem | null>(null)

const urgencyColor: Record<string, string> = { critical: 'red', urgent: 'orange', normal: 'default' }
const statusColor: Record<string, string> = { Pending: 'orange', Collecting: 'blue', Adopted: 'green', Closed: 'default' }
const slaColor: Record<string, string> = { normal: 'green', warning: 'gold', overdue: 'red', responded: 'blue' }
const visibilityColor: Record<string, string> = { all: 'green', dept: 'blue', personnel: 'purple' }

function goEdit() {
  router.push({ path: '/requirement/form', query: { id } })
}

onMounted(async () => {
  detail.value = await getRequirementDetail(id)
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
