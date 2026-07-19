<template>
  <div v-if="!detail" class="h-full p-[16px] bg-white rounded flex items-center justify-center">
    <Empty type="noData" />
  </div>
  <div v-else class="h-full p-[16px] bg-white rounded overflow-auto">
    <div class="flex items-center justify-between mb-4">
      <div class="flex items-center gap-2">
        <a-tag color="blue">{{ t('intel.industryDict.' + detail.industry) }}</a-tag>
        <h2 class="text-[18px] font-bold">{{ detail.title }}</h2>
      </div>
      <a-button @click="router.back()">{{ t('common.back') }}</a-button>
    </div>

    <a-descriptions bordered size="small" :column="2">
      <a-descriptions-item :label="t('intel.industry')">{{ t('intel.industryDict.' + detail.industry) }}</a-descriptions-item>
      <a-descriptions-item :label="t('common.createdAt')">{{ detail.createdAt ?? '--' }}</a-descriptions-item>
      <a-descriptions-item :label="t('intel.source')" :span="2">{{ detail.source ?? '--' }}</a-descriptions-item>
    </a-descriptions>

    <div class="mt-4">
      <h3 class="text-[15px] font-semibold mb-2">{{ t('intel.summary') }}</h3>
      <div class="leading-7 text-[hsl(var(--text))] whitespace-pre-wrap">{{ detail.summary ?? '--' }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Empty from '@q-web-plugin/empty'
import { getIndustryDetail } from '@/apis/intel/intelApi'
import type { IndustryIntelItem } from '@/apis/intel/types'

defineOptions({ name: 'IntelIndustryDetail' })
definePage({
  name: 'IntelIndustryDetail',
  meta: {
    layout: false,
    menu: false,
    title: 'intel.industry'
  } satisfies RouteMeta
})

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const id = route.query.id as string
const detail = ref<IndustryIntelItem | null>(null)

onMounted(async () => {
  detail.value = await getIndustryDetail(id)
})
</script>
