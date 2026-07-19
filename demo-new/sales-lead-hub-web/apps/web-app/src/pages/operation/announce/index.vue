<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col">
    <!-- 统计卡 -->
    <a-row :gutter="16" class="mb-3">
      <a-col :xs="12" :sm="6">
        <div class="stat-card"><a-statistic :title="t('announce.stat.total')" :value="stats.total" /></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-card"><a-statistic :title="t('announce.stat.published')" :value="stats.published" /></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-card"><a-statistic :title="t('announce.stat.draft')" :value="stats.draft" /></div>
      </a-col>
      <a-col :xs="12" :sm="6">
        <div class="stat-card"><a-statistic :title="t('announce.stat.totalViews')" :value="stats.totalViews" /></div>
      </a-col>
    </a-row>

    <div class="flex-1 min-h-0">
      <QBigTable
        ref="tableRef"
        :search-config="searchConfig"
        :toolbar-config="toolbarConfig"
        :columns="columns"
        :query-api="queryApi"
        height="100%"
      />
    </div>

    <!-- 预览 Drawer -->
    <a-drawer v-model:open="previewOpen" :title="t('announce.previewTitle')" width="520">
      <div v-if="current">
        <div class="flex items-center gap-2 mb-3">
          <a-tag v-if="current.isPinned" color="red">{{ t('announce.pinned') }}</a-tag>
          <a-tag :color="typeColor[current.type] ?? 'default'">{{ t('dict.announceType.' + current.type) }}</a-tag>
          <a-tag v-if="current.priority === 'important'" color="red">{{ t('dict.priority.important') }}</a-tag>
          <h3 class="text-[16px] font-bold">{{ current.title }}</h3>
        </div>
        <div class="text-[12px] text-[hsl(var(--secondary-text))] mb-3">
          {{ current.publisherName }} · {{ current.createdAt }} · {{ t('common.viewCount') }} {{ current.viewCount }}
        </div>
        <div class="rich-body" v-html="current.content"></div>
      </div>
    </a-drawer>

    <!-- 新建 / 编辑 Drawer 表单 -->
    <a-drawer
      v-model:open="formOpen"
      :title="isEdit ? t('announce.editTitle') : t('announce.add')"
      width="560"
      @close="closeForm"
    >
      <QForm ref="qFormRef" :schemas="schemas" v-model:model="formModel" />
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="closeForm">{{ t('common.cancel') }}</a-button>
          <a-button @click="handleSave('draft')">{{ t('common.saveDraft') }}</a-button>
          <a-button type="primary" @click="handleSave('published')">{{ t('common.publish') }}</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { QBigTable } from '@/components/q-big-table'
import type { QBigTableExpose, ToolbarButton, TableColumn } from '@/components/q-big-table'
import { QForm } from '@/components/q-form'
import type { QFormExpose } from '@/components/q-form'
import {
  getAnnounceList,
  getAnnounceStats,
  getAnnounceDetail,
  createAnnounce,
  updateAnnounce,
  changeAnnounceStatus,
  deleteAnnounce
} from '@/apis/announce/announceApi'
import type { AnnounceItem, AnnounceStats } from '@/apis/announce/types'

defineOptions({ name: 'OperationAnnounce' })
definePage({
  name: 'OperationAnnounce',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.announce.DEFAULT'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const tableRef = ref<QBigTableExpose | null>(null)

const typeColor: Record<string, string> = { notice: 'blue', policy: 'default', activity: 'green', other: 'default' }
const statusColor: Record<string, string> = { draft: 'default', scheduled: 'orange', published: 'green', withdrawn: 'red' }

const stats = reactive<AnnounceStats>({ total: 0, published: 0, draft: 0, totalViews: 0 })

const typeOptions = computed(() => [
  { label: t('dict.announceType.notice'), value: 'notice' },
  { label: t('dict.announceType.policy'), value: 'policy' },
  { label: t('dict.announceType.activity'), value: 'activity' },
  { label: t('dict.announceType.other'), value: 'other' }
])
const statusOptions = computed(() => [
  { label: t('dict.announceStatus.draft'), value: 'draft' },
  { label: t('dict.announceStatus.scheduled'), value: 'scheduled' },
  { label: t('dict.announceStatus.published'), value: 'published' },
  { label: t('dict.announceStatus.withdrawn'), value: 'withdrawn' }
])
const priorityOptions = computed(() => [
  { label: t('dict.priority.normal'), value: 'normal' },
  { label: t('dict.priority.important'), value: 'important' }
])

const searchConfig = computed(() => [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('announce.searchPlaceholder'), allowClear: true, maxlength: 100 }
  },
  {
    field: 'type',
    label: t('announce.type'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: typeOptions.value }
  },
  {
    field: 'status',
    label: t('common.status'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: statusOptions.value }
  }
])

const toolbarConfig: ToolbarButton[] = [
  { label: t('announce.add'), type: 'primary', onClick: () => openCreate() }
]

const columns: TableColumn[] = [
  {
    field: 'title',
    title: t('announce.title'),
    minWidth: 260,
    slots: {
      default: ({ row }: any) => (
        <span class="flex items-center gap-1">
          {row.isPinned ? <a-tag color="red">{t('announce.pinned')}</a-tag> : null}
          <a class="text-[hsl(var(--primary))]" onClick={() => openPreview(row.id)}>{row.title}</a>
        </span>
      )
    }
  },
  {
    field: 'type',
    title: t('announce.type'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={typeColor[row.type] ?? 'default'}>{t('dict.announceType.' + row.type)}</a-tag> }
  },
  {
    field: 'status',
    title: t('common.status'),
    width: 110,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] ?? 'default'}>{t('dict.announceStatus.' + row.status)}</a-tag> }
  },
  {
    field: 'priority',
    title: t('announce.priority'),
    width: 90,
    slots: { default: ({ row }: any) => <a-tag color={row.priority === 'important' ? 'red' : 'default'}>{t('dict.priority.' + row.priority)}</a-tag> }
  },
  { field: 'publisherName', title: t('announce.publisher'), width: 110 },
  { field: 'viewCount', title: t('announce.views'), width: 90 },
  { field: 'createdAt', title: t('common.createdAt'), width: 160 },
  {
    title: t('common.action'),
    width: 220,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => (
        <span class="flex gap-1 flex-wrap">
          <a-button type="link" size="small" onClick={() => openPreview(row.id)}>{t('common.view')}</a-button>
          <a-button type="link" size="small" onClick={() => openEdit(row.id)}>{t('common.edit')}</a-button>
          {row.status !== 'published' ? (
            <a-button type="link" size="small" onClick={() => changeStatus(row.id, 'published')}>{t('common.publish')}</a-button>
          ) : (
            <a-button type="link" size="small" danger onClick={() => changeStatus(row.id, 'withdrawn')}>{t('announce.withdraw')}</a-button>
          )}
          <a-button type="link" size="small" danger onClick={() => handleDelete(row.id)}>{t('common.delete')}</a-button>
        </span>
      )
    }
  }
]

// ============ 预览 ============
const previewOpen = ref(false)
const current = ref<AnnounceItem | null>(null)
async function openPreview(id: string) {
  current.value = await getAnnounceDetail(id)
  previewOpen.value = true
}

// ============ 新建 / 编辑 表单 ============
const formOpen = ref(false)
const qFormRef = ref<QFormExpose | null>(null)
const editId = ref<string | null>(null)
const isEdit = computed(() => !!editId.value)
const formModel = reactive<{
  title: string
  type: string
  priority: string
  isPinned: boolean
  bannerEnabled: boolean
  content: string
}>({
  title: '',
  type: 'notice',
  priority: 'normal',
  isPinned: false,
  bannerEnabled: false,
  content: ''
})

const schemas = computed(() => [
  {
    field: 'title',
    label: t('announce.title'),
    component: 'Input',
    rules: [{ required: true, message: t('announce.titlePlaceholder') }],
    componentProps: { placeholder: t('announce.titlePlaceholder'), maxlength: 100, showCount: true }
  },
  {
    field: 'type',
    label: t('announce.type'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), options: typeOptions.value, style: 'width:100%' }
  },
  {
    field: 'priority',
    label: t('announce.priority'),
    component: 'RadioGroup',
    componentProps: { options: priorityOptions.value }
  },
  {
    field: 'isPinned',
    label: t('announce.pinned'),
    component: 'Switch',
    valuePropName: 'checked',
    updateEventName: 'update:checked'
  },
  {
    field: 'bannerEnabled',
    label: t('announce.banner'),
    component: 'Switch',
    valuePropName: 'checked',
    updateEventName: 'update:checked'
  },
  {
    field: 'content',
    label: t('announce.content'),
    component: 'Textarea',
    componentProps: { placeholder: t('announce.contentPlaceholder'), rows: 6 }
  }
])

function resetForm() {
  editId.value = null
  formModel.title = ''
  formModel.type = 'notice'
  formModel.priority = 'normal'
  formModel.isPinned = false
  formModel.bannerEnabled = false
  formModel.content = ''
}
function openCreate() {
  resetForm()
  formOpen.value = true
}
async function openEdit(id: string) {
  const d = await getAnnounceDetail(id)
  editId.value = id
  formModel.title = d.title
  formModel.type = d.type
  formModel.priority = d.priority || 'normal'
  formModel.isPinned = d.isPinned
  formModel.bannerEnabled = d.bannerEnabled
  formModel.content = d.content
  formOpen.value = true
}
function closeForm() {
  formOpen.value = false
}
async function handleSave(status: string) {
  await qFormRef.value?.validate()
  if (isEdit.value) {
    await updateAnnounce({ id: editId.value as string, ...formModel })
  } else {
    await createAnnounce({ ...formModel })
  }
  message.success(status === 'draft' ? t('common.success') : t('announce.publishSuccess'))
  formOpen.value = false
  refreshAll()
}

// ============ 状态 / 删除 ============
async function changeStatus(id: string, status: string) {
  await changeAnnounceStatus(id, status)
  message.success(t('common.success'))
  refreshAll()
}
function handleDelete(id: string) {
  Modal.confirm({
    title: t('announce.deleteConfirm'),
    onOk: async () => {
      await deleteAnnounce(id)
      message.success(t('common.success'))
      refreshAll()
    }
  })
}

async function queryApi({ page }: any, searchParams: any) {
  const result = await getAnnounceList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
}

async function refreshAll() {
  tableRef.value?.refresh()
  Object.assign(stats, await getAnnounceStats())
}

onMounted(async () => {
  Object.assign(stats, await getAnnounceStats())
})
</script>

<style scoped>
.stat-card {
  padding: 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  background: hsl(var(--card-bg));
}

.rich-body {
  line-height: 1.8;
  color: hsl(var(--text));
}
</style>
