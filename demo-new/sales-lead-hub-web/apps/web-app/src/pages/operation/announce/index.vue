<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col overflow-auto">
    <!-- 统计卡（点击筛选状态） -->
    <div class="stat-row">
      <StatCard :label="t('announce.stat.total')" :value="stats.total" accent="#1890ff" clickable
        :active="statusFilter === ''" @click="statusFilter = ''">
        <template #icon><SoundOutlined /></template>
      </StatCard>
      <StatCard :label="t('announce.stat.published')" :value="stats.published" accent="#52c41a" clickable
        :active="statusFilter === 'published'" @click="statusFilter = 'published'">
        <template #icon><CheckCircleOutlined /></template>
      </StatCard>
      <StatCard :label="t('announce.stat.draft')" :value="stats.draft" accent="#faad14" clickable
        :active="statusFilter === 'draft'" @click="statusFilter = 'draft'">
        <template #icon><EditOutlined /></template>
      </StatCard>
      <StatCard :label="t('announce.stat.totalViews')" :value="stats.totalViews" accent="#2f54eb">
        <template #icon><EyeOutlined /></template>
      </StatCard>
    </div>

    <!-- 工具栏 -->
    <div class="flex items-center justify-between mb-3 flex-wrap gap-2">
      <div class="flex items-center gap-2 flex-wrap">
        <a-input-search v-model:value="keyword" :placeholder="t('announce.searchPlaceholder')" allow-clear class="w-[240px]" />
        <a-select :value="typeFilter" class="w-[140px]" allow-clear :placeholder="t('announce.type')"
          @update:value="(v: any) => (typeFilter = v ?? '')">
          <a-select-option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
        </a-select>
        <a-select :value="statusFilter" class="w-[130px]" allow-clear :placeholder="t('common.status')"
          @update:value="(v: any) => (statusFilter = v ?? '')">
          <a-select-option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
        </a-select>
      </div>
      <div class="flex items-center gap-2">
        <a-button v-if="selectedRows.length" danger @click="handleBatchDelete">
          <template #icon><DeleteOutlined /></template>
          {{ t('common.batchDelete') }}（{{ selectedRows.length }}）
        </a-button>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          {{ t('announce.add') }}
        </a-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="flex-1 min-h-0">
      <QBigTable
        ref="tableRef"
        :data="filtered"
        :columns="columns"
        :selectable="true"
        :pagination-config="{ pageSize: 10 }"
        height="100%"
        @checkbox-change="onCheckboxChange"
      />
    </div>

    <!-- 预览 Drawer -->
    <a-drawer v-model:open="previewOpen" :title="t('announce.previewTitle')" width="520">
      <div v-if="current">
        <div class="flex items-center gap-2 mb-3 flex-wrap">
          <a-tag v-if="current.isPinned" color="red">{{ t('announce.pinned') }}</a-tag>
          <a-tag :color="typeColor[current.type] || 'default'">{{ t('dict.announceType.' + current.type) }}</a-tag>
          <a-tag v-if="current.priority === 'high'" color="red">{{ t('dict.priority.high') }}</a-tag>
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
      width="640"
      @close="closeForm"
    >
      <div class="form-grid">
        <div class="form-item">
          <label>{{ t('announce.title') }}</label>
          <a-input v-model:value="formModel.title" :placeholder="t('announce.titlePlaceholder')" :maxlength="100" show-count />
        </div>
        <div class="flex gap-4">
          <div class="form-item flex-1">
            <label>{{ t('announce.type') }}</label>
            <a-select :value="formModel.type" class="w-full" @update:value="(v: any) => (formModel.type = v)">
              <a-select-option v-for="o in typeOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
            </a-select>
          </div>
          <div class="form-item flex-1">
            <label>{{ t('announce.priority') }}</label>
            <a-radio-group v-model:value="formModel.priority">
              <a-radio value="normal">{{ t('dict.priority.normal') }}</a-radio>
              <a-radio value="high">{{ t('dict.priority.high') }}</a-radio>
            </a-radio-group>
          </div>
        </div>
        <div class="flex gap-6">
          <div class="flex items-center gap-2">
            <a-switch v-model:checked="formModel.isPinned" />
            <span>{{ t('announce.pinned') }}</span>
          </div>
          <div class="flex items-center gap-2">
            <a-switch v-model:checked="formModel.bannerEnabled" />
            <span>{{ t('announce.banner') }}</span>
          </div>
        </div>
        <div class="form-item">
          <label>{{ t('announce.content') }}</label>
          <RichEditor v-model:modelValue="formModel.content" :placeholder="t('announce.contentPlaceholder')" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="closeForm">{{ t('common.cancel') }}</a-button>
          <a-button @click="saveDraft">{{ t('common.saveDraft') }}</a-button>
          <a-button type="primary" @click="requestPublishForm">{{ t('common.publish') }}</a-button>
        </div>
      </template>
    </a-drawer>

    <!-- 发布确认弹窗 -->
    <a-modal
      v-model:open="publishModalOpen"
      :title="t('announce.publishConfirmTitle')"
      :ok-text="t('common.publish')"
      centered
      @ok="confirmPublish"
    >
      <p class="mb-2">{{ t('announce.publishConfirmContent') }}</p>
      <p class="text-[13px] text-[hsl(var(--secondary-text))]">{{ t('announce.publishConfirmHint') }}</p>
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, DeleteOutlined, SoundOutlined, CheckCircleOutlined, EditOutlined, EyeOutlined
} from '@ant-design/icons-vue'
import StatCard from '@/components/stat-card/index.vue'
import RichEditor from '@/components/rich-editor/index.vue'
import { QBigTable } from '@/components/q-big-table'
import type { TableColumn, QBigTableExpose } from '@/components/q-big-table'
import {
  getAnnounceList,
  getAnnounceDetail,
  createAnnounce,
  updateAnnounce,
  changeAnnounceStatus,
  deleteAnnounce
} from '@/apis/announce/announceApi'
import type { AnnounceItem } from '@/apis/announce/types'

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

const typeColor: Record<string, string> = { notice: 'blue', policy: 'purple', activity: 'green', other: 'default' }
const statusColor: Record<string, string> = { draft: 'default', published: 'green', archived: 'orange' }

const allItems = ref<AnnounceItem[]>([])
const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')

// ============ 表格选择 ============
const tableRef = ref<QBigTableExpose>()
const selectedRows = ref<AnnounceItem[]>([])
function onCheckboxChange() {
  selectedRows.value = (tableRef.value?.getCheckboxRecords() ?? []) as AnnounceItem[]
}
function clearSelection() {
  tableRef.value?.clearCheckboxRow()
  selectedRows.value = []
}

const typeOptions = computed(() => [
  { label: t('dict.announceType.notice'), value: 'notice' },
  { label: t('dict.announceType.policy'), value: 'policy' },
  { label: t('dict.announceType.activity'), value: 'activity' },
  { label: t('dict.announceType.other'), value: 'other' }
])
const statusOptions = computed(() => [
  { label: t('dict.announceStatus.draft'), value: 'draft' },
  { label: t('dict.announceStatus.published'), value: 'published' },
  { label: t('dict.announceStatus.archived'), value: 'archived' }
])

function renderTitle(row: AnnounceItem) {
  return (
    <span class="inline-flex items-center gap-1">
      {row.isPinned ? <a-tag color="red">{t('announce.pinned')}</a-tag> : null}
      <a class="text-[hsl(var(--primary))]" onClick={() => openPreview(row.id)}>{row.title}</a>
    </span>
  )
}
function renderAction(row: AnnounceItem) {
  return (
    <span class="flex gap-1 flex-wrap">
      <a-button type="link" size="small" onClick={() => openPreview(row.id)}>{t('common.view')}</a-button>
      <a-button type="link" size="small" onClick={() => openEdit(row)}>{t('common.edit')}</a-button>
      {row.status === 'draft'
        ? <a-button type="link" size="small" style={{ color: 'hsl(var(--primary))' }} onClick={() => requestPublish(row)}>{t('common.publish')}</a-button>
        : row.status === 'published'
          ? <a-button type="link" size="small" danger onClick={() => changeStatus(row, 'archived')}>{t('announce.withdraw')}</a-button>
          : row.status === 'archived'
            ? <a-button type="link" size="small" style={{ color: 'hsl(var(--primary))' }} onClick={() => changeStatus(row, 'published')}>{t('announce.republish')}</a-button>
            : null}
      <a-popconfirm title={t('announce.deleteConfirm')} onConfirm={() => handleDelete(row.id)}>
        <a-button type="link" size="small" danger>{t('common.delete')}</a-button>
      </a-popconfirm>
    </span>
  )
}

const columns = computed<TableColumn[]>(() => [
  { field: 'id', title: t('announce.no'), width: 110 },
  { field: 'title', title: t('announce.title'), minWidth: 240, slots: { default: ({ row }: any) => renderTitle(row) } },
  {
    field: 'type',
    title: t('announce.type'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={typeColor[row.type] || 'default'}>{t('dict.announceType.' + row.type)}</a-tag> }
  },
  {
    field: 'status',
    title: t('common.status'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] || 'default'}>{t('dict.announceStatus.' + row.status)}</a-tag> }
  },
  {
    field: 'priority',
    title: t('announce.priority'),
    width: 90,
    slots: {
      default: ({ row }: any) =>
        row.priority === 'high'
          ? <a-tag color="red">{t('dict.priority.high')}</a-tag>
          : <span class="text-[hsl(var(--secondary-text))]">{t('dict.priority.normal')}</span>
    }
  },
  { field: 'publisherName', title: t('announce.publisher'), width: 110 },
  { field: 'createdAt', title: t('common.createdAt'), width: 160 },
  {
    field: 'publishedAt',
    title: t('common.publishedAt'),
    width: 160,
    slots: { default: ({ row }: any) => <span class={row.publishedAt ? '' : 'text-[hsl(var(--secondary-text))]'}>{row.publishedAt || '—'}</span> }
  },
  { field: 'viewCount', title: t('announce.views'), width: 90 },
  { field: 'action', title: t('common.action'), width: 240, fixed: 'right', slots: { default: ({ row }: any) => renderAction(row) } }
])

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return allItems.value.filter((i) => {
    if (kw && !i.title.toLowerCase().includes(kw)) return false
    if (typeFilter.value && i.type !== typeFilter.value) return false
    if (statusFilter.value && i.status !== statusFilter.value) return false
    return true
  })
})

const stats = computed(() => ({
  total: allItems.value.length,
  published: allItems.value.filter((i) => i.status === 'published').length,
  draft: allItems.value.filter((i) => i.status === 'draft').length,
  totalViews: allItems.value.reduce((s, i) => s + (i.viewCount || 0), 0)
}))

async function load() {
  const res = await getAnnounceList({ pageNumber: 1, pageSize: 999 })
  allItems.value = res.records
}

// ============ 预览 ============
const previewOpen = ref(false)
const current = ref<AnnounceItem | null>(null)
async function openPreview(id: string) {
  current.value = allItems.value.find((i) => i.id === id) ?? (await getAnnounceDetail(id))
  previewOpen.value = true
}

// ============ 新建 / 编辑 ============
const formOpen = ref(false)
const editId = ref<string | null>(null)
const isEdit = computed(() => !!editId.value)
const formModel = reactive<{
  title: string
  type: string
  priority: string
  isPinned: boolean
  bannerEnabled: boolean
  content: string
  // 乐观锁版本号：编辑态由 openEdit 从行数据回填，随 update 提交
  version: number
}>({
  title: '',
  type: 'notice',
  priority: 'normal',
  isPinned: false,
  bannerEnabled: false,
  content: '',
  version: 0
})

function resetForm() {
  editId.value = null
  formModel.title = ''
  formModel.type = 'notice'
  formModel.priority = 'normal'
  formModel.isPinned = false
  formModel.bannerEnabled = false
  formModel.content = ''
  formModel.version = 0
}
function openCreate() {
  resetForm()
  formOpen.value = true
}
function openEdit(row: AnnounceItem) {
  editId.value = row.id
  formModel.title = row.title
  formModel.type = row.type
  formModel.priority = row.priority || 'normal'
  formModel.isPinned = row.isPinned
  formModel.bannerEnabled = row.bannerEnabled
  formModel.content = row.content
  // 乐观锁：回填行数据的 version，随 update 原样提交
  formModel.version = row.version
  formOpen.value = true
}
function closeForm() {
  formOpen.value = false
}

function validForm(): boolean {
  if (!formModel.title.trim()) {
    message.warning(t('announce.titlePlaceholder'))
    return false
  }
  if (!formModel.content || !formModel.content.replace(/<[^>]*>/g, '').trim()) {
    message.warning(t('announce.contentPlaceholder'))
    return false
  }
  return true
}

// 抽取当前表单的创建入参（不含 id/version，供 create 与 update 共用）
function buildCreateParams() {
  return {
    title: formModel.title,
    type: formModel.type,
    priority: formModel.priority,
    isPinned: formModel.isPinned,
    bannerEnabled: formModel.bannerEnabled,
    content: formModel.content
  }
}

async function saveDraft() {
  if (!validForm()) return
  try {
    if (isEdit.value) {
      await updateAnnounce({ id: editId.value as string, version: formModel.version, ...buildCreateParams() })
    } else {
      // 后端 create 一律落草稿，存草稿即普通新建
      await createAnnounce(buildCreateParams())
    }
  } catch {
    // 请求失败（含版本冲突）：拦截器已弹 message，保留抽屉让用户重试
    return
  }
  formOpen.value = false
  message.success(t('common.success'))
  await load()
}

// ============ 发布确认 ============
const publishModalOpen = ref(false)
const publishFromForm = ref(false)
const publishTargetId = ref<string | null>(null)

function requestPublishForm() {
  if (!validForm()) return
  publishFromForm.value = true
  publishModalOpen.value = true
}
function requestPublish(row: AnnounceItem) {
  publishFromForm.value = false
  publishTargetId.value = row.id
  publishModalOpen.value = true
}
async function confirmPublish() {
  if (publishFromForm.value) {
    // 后端 create/update 只落草稿，发布须紧跟 changeStatus(published)——两步串联
    let targetId: string
    // 第一步：落库（create/update）。失败则保留抽屉重试；create 失败时 editId 仍为 null，重试仍是 create（正确）。
    try {
      if (isEdit.value) {
        await updateAnnounce({ id: editId.value as string, version: formModel.version, ...buildCreateParams() })
        targetId = editId.value as string
      } else {
        targetId = await createAnnounce(buildCreateParams())
        // 关键：create 成功立即切编辑态，杜绝 changeStatus 失败后重试造成重复 create
        editId.value = targetId
      }
    } catch {
      publishModalOpen.value = false
      return
    }
    // 第二步：发布。此时草稿已落库，失败则切回列表并提示去列表重试，避免重复 create。
    try {
      await changeAnnounceStatus(targetId, 'published')
    } catch {
      publishModalOpen.value = false
      formOpen.value = false
      message.warning(t('announce.publishDraftSaved'))
      await load()
      return
    }
    publishModalOpen.value = false
    formOpen.value = false
  } else if (publishTargetId.value) {
    try {
      await changeAnnounceStatus(publishTargetId.value, 'published')
    } catch {
      publishModalOpen.value = false
      return
    }
    publishModalOpen.value = false
  } else {
    publishModalOpen.value = false
    return
  }
  message.success(t('announce.publishSuccess'))
  await load()
}

// ============ 状态 / 删除 ============
async function changeStatus(row: AnnounceItem, status: string) {
  try {
    await changeAnnounceStatus(row.id, status)
  } catch {
    return
  }
  message.success(t('common.success'))
  await load()
}
async function handleDelete(id: string) {
  try {
    await deleteAnnounce(id)
  } catch {
    return
  }
  message.success(t('common.success'))
  await load()
}
function handleBatchDelete() {
  if (!selectedRows.value.length) return
  Modal.confirm({
    title: t('announce.batchDeleteConfirm', { n: selectedRows.value.length }),
    okType: 'danger',
    onOk: async () => {
      const ids = [...new Set(selectedRows.value.map((r) => r.id))]
      try {
        await Promise.all(ids.map((id) => deleteAnnounce(id)))
      } catch {
        return
      }
      clearSelection()
      message.success(t('common.success'))
      await load()
    }
  })
}

onMounted(load)
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-item > label {
  font-size: 13px;
  font-weight: 500;
  color: hsl(var(--text));
}
.rich-body {
  line-height: 1.8;
  color: hsl(var(--text));
}
</style>
