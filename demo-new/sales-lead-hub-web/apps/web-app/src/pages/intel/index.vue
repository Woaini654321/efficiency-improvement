<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col">
    <a-segmented v-model:value="activeTab" :options="tabOptions" class="mb-3 self-start" />
    <div class="flex-1 min-h-0">
      <QBigTable
        v-if="activeTab === 'competitor'"
        key="competitor"
        :search-config="competitorSearch"
        :toolbar-config="toolbarConfig"
        :columns="competitorColumns"
        :query-api="competitorQuery"
        height="100%"
      />
      <QBigTable
        v-else
        key="industry"
        :search-config="industrySearch"
        :columns="industryColumns"
        :query-api="industryQuery"
        height="100%"
      />
    </div>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { QBigTable } from '@/components/q-big-table'
import type { ToolbarButton, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { getCompetitorList, getIndustryList } from '@/apis/intel/intelApi'

defineOptions({ name: 'IntelList' })
definePage({
  name: 'IntelList',
  meta: {
    layout: false,
    menu: true,
    title: 'intel.list'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()
const activeTab = ref<'competitor' | 'industry'>('competitor')

const tabOptions = computed(() => [
  { label: t('intel.tabCompetitor'), value: 'competitor' },
  { label: t('intel.tabIndustry'), value: 'industry' }
])

const intelTypeColor: Record<string, string> = {
  new_product: 'green',
  price_change: 'orange',
  customer_case: 'blue',
  other: 'default'
}

// ============ 竞品情报 ============
const brandOptions = computed(() =>
  ['Sierra Wireless', 'Telit', '广和通', '移远', 'u-blox', 'Thales'].map((b) => ({ label: b, value: b }))
)
const intelTypeOptions = computed(() => [
  { label: t('dict.intelType.new_product'), value: 'new_product' },
  { label: t('dict.intelType.price_change'), value: 'price_change' },
  { label: t('dict.intelType.customer_case'), value: 'customer_case' },
  { label: t('dict.intelType.other'), value: 'other' }
])

const competitorSearch: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('intel.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'brand',
    label: t('intel.brand'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:170px', options: brandOptions.value }
  },
  {
    field: 'intelType',
    label: t('intel.intelType'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:150px', options: intelTypeOptions.value }
  }
]

const toolbarConfig: ToolbarButton[] = [
  { label: t('intel.submit'), type: 'primary', onClick: () => { router.push({ path: '/intel/submit' }) } }
]

const competitorColumns: TableColumn[] = [
  {
    field: 'title',
    title: t('intel.title'),
    minWidth: 300,
    slots: {
      default: ({ row }: any) => (
        <a class="text-[hsl(var(--primary))]" onClick={() => goCompetitor(row.id)}>{row.title}</a>
      )
    }
  },
  { field: 'brand', title: t('intel.brand'), width: 130 },
  { field: 'product', title: t('intel.product'), width: 130 },
  {
    field: 'intelType',
    title: t('intel.intelType'),
    width: 120,
    slots: { default: ({ row }: any) => <a-tag color={intelTypeColor[row.intelType] ?? 'default'}>{t('dict.intelType.' + row.intelType)}</a-tag> }
  },
  { field: 'source', title: t('intel.source'), minWidth: 150 },
  { field: 'submitterName', title: t('intel.submitter'), width: 110 },
  { field: 'createdAt', title: t('common.createdAt'), width: 170 },
  {
    title: t('common.action'),
    width: 100,
    fixed: 'right',
    slots: { default: ({ row }: any) => <a-button type="link" onClick={() => goCompetitor(row.id)}>{t('common.view')}</a-button> }
  }
]

// ============ 行业情报 ============
const industryOptions = computed(() => [
  { label: t('intel.industryDict.trend'), value: 'trend' },
  { label: t('intel.industryDict.automotive'), value: 'automotive' },
  { label: t('intel.industryDict.policy'), value: 'policy' },
  { label: t('intel.industryDict.energy'), value: 'energy' },
  { label: t('intel.industryDict.industrial'), value: 'industrial' },
  { label: t('intel.industryDict.smartcity'), value: 'smartcity' }
])

const industrySearch: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('intel.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'industry',
    label: t('intel.industry'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:170px', options: industryOptions.value }
  }
]

const industryColumns: TableColumn[] = [
  {
    field: 'title',
    title: t('intel.title'),
    minWidth: 320,
    slots: {
      default: ({ row }: any) => (
        <a class="text-[hsl(var(--primary))]" onClick={() => goIndustry(row.id)}>{row.title}</a>
      )
    }
  },
  {
    field: 'industry',
    title: t('intel.industry'),
    width: 140,
    slots: { default: ({ row }: any) => <a-tag color="blue">{t('intel.industryDict.' + row.industry)}</a-tag> }
  },
  { field: 'source', title: t('intel.source'), minWidth: 170 },
  { field: 'createdAt', title: t('common.createdAt'), width: 170 },
  {
    title: t('common.action'),
    width: 100,
    fixed: 'right',
    slots: { default: ({ row }: any) => <a-button type="link" onClick={() => goIndustry(row.id)}>{t('common.view')}</a-button> }
  }
]

function goCompetitor(id: string) {
  router.push({ path: '/intel/competitor', query: { id } })
}
function goIndustry(id: string) {
  router.push({ path: '/intel/industry', query: { id } })
}

async function competitorQuery({ page }: any, searchParams: any) {
  const result = await getCompetitorList({ ...searchParams, pageNumber: page.pageNumber, pageSize: page.pageSize })
  return { result: result.records, page: { total: result.total } }
}
async function industryQuery({ page }: any, searchParams: any) {
  const result = await getIndustryList({ ...searchParams, pageNumber: page.pageNumber, pageSize: page.pageSize })
  return { result: result.records, page: { total: result.total } }
}
</script>
