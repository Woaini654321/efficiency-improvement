<template>
  <div v-if="!detail" class="h-full p-[16px] bg-white rounded flex items-center justify-center">
    <Empty type="noData" />
  </div>
  <div v-else class="h-full p-[16px] bg-white rounded overflow-auto">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <a-tag v-if="detail.isPinned" color="red">{{ t('opportunity.pinned') }}</a-tag>
        <a-tag :color="typeColor[detail.type] ?? 'default'">{{ t('dict.oppType.' + detail.type) }}</a-tag>
        <a-tag :color="statusColor[detail.status] ?? 'default'">{{ t('dict.oppStatus.' + detail.status) }}</a-tag>
        <h2 class="text-[18px] font-bold">{{ detail.title }}</h2>
      </div>
      <div class="flex gap-2">
        <a-button @click="goEdit">{{ t('common.edit') }}</a-button>
        <a-button @click="goCopy">{{ t('opportunity.copyAsNew') }}</a-button>
        <a-button @click="router.back()">{{ t('common.back') }}</a-button>
      </div>
    </div>

    <a-descriptions bordered size="small" :column="2">
      <a-descriptions-item :label="t('opportunity.publisher')">{{ detail.publisherName ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('opportunity.dept')">{{ detail.publisherDeptName ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('opportunity.category')" :span="2">
        <a-tag v-for="c in detail.categoryNames" :key="c">{{ c }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item :label="t('opportunity.summary')" :span="2">{{ detail.summary ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('common.publishedAt')">{{ detail.publishedAt ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('opportunity.metrics')">
        {{ t('common.viewCount') }} {{ detail.viewCount }} · {{ t('common.like') }} {{ detail.likeCount }} ·
        {{ t('common.collect') }} {{ detail.collectCount }} · {{ t('common.comment') }} {{ detail.commentCount }}
      </a-descriptions-item>
    </a-descriptions>

    <div class="mt-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('opportunity.content') }}</h3>
      <!-- mock 富文本内容展示（后端接入后由富文本字段渲染） -->
      <div class="rich-body" v-html="detail.content"></div>
    </div>

    <div v-if="detail.attachments.length" class="mt-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('opportunity.attachments') }}</h3>
      <QFileList :file-list="detail.attachments" :allow-delete="false" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Empty from '@q-web-plugin/empty'
import QFileList from '@/components/q-file-list/index.vue'
import { getOpportunityDetail } from '@/apis/opportunity/opportunityApi'
import type { OpportunityItem } from '@/apis/opportunity/types'

defineOptions({ name: 'OpportunityDetail' })
definePage({
  name: 'OpportunityDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'opportunity.detail'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<OpportunityItem | null>(null)

const typeColor: Record<string, string> = { product_info: 'blue', solution: 'green', success_case: 'orange' }
const statusColor: Record<string, string> = { published: 'green', archived: 'red', draft: 'default' }

function goEdit() {
  router.push({ path: '/opportunity/form', query: { id } })
}
function goCopy() {
  router.push({ path: '/opportunity/form', query: { copyFrom: id } })
}

onMounted(async () => {
  detail.value = await getOpportunityDetail(id)
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
