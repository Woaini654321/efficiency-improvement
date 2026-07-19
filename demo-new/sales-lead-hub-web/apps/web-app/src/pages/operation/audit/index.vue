<template>
  <div class="h-full p-[16px] bg-white rounded flex flex-col overflow-hidden">
    <h2 class="text-[18px] font-bold mb-4">{{ t('audit.pageTitle') }}</h2>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <a-segmented v-model:value="segType" :options="segOptions" @change="onSegChange" />
      <a-input v-model:value="keyword" :placeholder="t('audit.searchPlaceholder')" allow-clear class="w-[200px]">
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <a-select :value="statusFilter" :placeholder="t('audit.statusFilter')" allow-clear class="w-[140px]"
        :options="statusOptions" @update:value="(v: any) => (statusFilter = v)" />
      <a-select :value="publisherFilter" :placeholder="t('audit.publisher')" allow-clear class="w-[130px]"
        :options="publisherOptions" @update:value="(v: any) => (publisherFilter = v)" />
      <a-range-picker :value="(dateRange as any)" value-format="YYYY-MM-DD" class="w-[240px]" allow-clear
        @update:value="(v: any) => (dateRange = v || [])" />
      <a-select :value="sortField" class="w-[110px]" :options="sortFieldOptions"
        @update:value="(v: any) => (sortField = v)" />
      <a-button :title="sortOrder === 'descend' ? t('audit.desc') : t('audit.asc')"
        @click="sortOrder = sortOrder === 'descend' ? 'ascend' : 'descend'">
        <template #icon><SortAscendingOutlined v-if="sortOrder === 'ascend'" /><SortDescendingOutlined v-else /></template>
      </a-button>
      <div class="flex-1" />
      <a-button v-if="sortDirty" type="primary" size="small" @click="saveInlineSort">
        <template #icon><SaveOutlined /></template>{{ t('audit.saveSort') }}
      </a-button>
      <a-button @click="openSortModal">
        <template #icon><OrderedListOutlined /></template>{{ t('audit.manualSort') }}
      </a-button>
      <a-button type="primary" @click="openProxy">
        <template #icon><UserAddOutlined /></template>{{ t('audit.proxyPublish') }}
      </a-button>
    </div>

    <!-- 表格 -->
    <div class="flex-1 min-h-0">
      <QBigTable
        ref="tableRef"
        :data="filteredData"
        :columns="columns"
        :selectable="true"
        :grid-config="gridConfig"
        :pagination-config="{ pageSize: 10 }"
        height="100%"
        @checkbox-change="onCheckboxChange"
      />
    </div>

    <!-- 批量操作栏 -->
    <div v-if="selectedRows.length" class="batch-bar">
      <span>{{ t('audit.selectedN', { n: selectedRows.length }) }}</span>
      <a-button size="small" @click="batchOffline">{{ t('audit.batchArchive') }}</a-button>
      <a-button size="small" danger @click="batchDelete">{{ t('audit.batchDelete') }}</a-button>
      <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('audit.batchLimit') }}</span>
    </div>

    <!-- 下架原因 Modal -->
    <a-modal v-model:open="offlineOpen" :title="t('audit.offlineTitle')" @ok="confirmOffline">
      <div class="mb-2">{{ t('audit.offlineReasonLabel') }}</div>
      <a-textarea v-model:value="offlineReason" :rows="4" :maxlength="200" show-count
        :placeholder="t('audit.offlineReasonPlaceholder')" />
      <div class="mt-2 text-[12px] text-[hsl(var(--secondary-text))]">{{ t('audit.offlineHint') }}</div>
    </a-modal>

    <!-- 手动排序 Modal -->
    <a-modal v-model:open="sortModalOpen" :title="t('audit.sortModalTitle', { type: segLabel })" :width="620" @ok="saveSortModal">
      <div class="mb-3 text-[12px] text-[hsl(var(--secondary-text))]">{{ t('audit.sortModalHint') }}</div>
      <div class="sort-list">
        <div v-for="(item, i) in sortList" :key="item.id" class="sort-item">
          <span class="w-[28px] text-center text-[hsl(var(--secondary-text))]">{{ i + 1 }}</span>
          <a-tag :color="item.contentType === 'opportunity' ? 'blue' : 'purple'">
            {{ t('dict.auditContentType.' + item.contentType) }}
          </a-tag>
          <span class="flex-1 truncate">
            <a-tag v-if="item.isPinned" color="red">{{ t('audit.pinned') }}</a-tag>{{ item.title }}
          </span>
          <a-button type="text" size="small" :disabled="i === 0" @click="modalUp(i)"><UpOutlined /></a-button>
          <a-button type="text" size="small" :disabled="i === sortList.length - 1" @click="modalDown(i)"><DownOutlined /></a-button>
        </div>
        <Empty v-if="!sortList.length" type="noData" />
      </div>
    </a-modal>

    <!-- 替发方案 Modal -->
    <a-modal v-model:open="proxyOpen" :title="t('audit.proxyPublish')" :width="680" @ok="submitProxy">
      <div class="form-grid">
        <div>
          <label class="fld-label">{{ t('audit.fPublisher') }} *</label>
          <a-select :value="proxyForm.publisher" class="w-full" :options="publisherPickOptions"
            @update:value="(v: any) => (proxyForm.publisher = v)" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fTitle') }} *</label>
          <a-input v-model:value="proxyForm.title" :maxlength="100" show-count :placeholder="t('audit.fTitlePlaceholder')" />
        </div>
        <div class="two-col">
          <div>
            <label class="fld-label">{{ t('audit.fType') }}</label>
            <a-select :value="proxyForm.contentType" class="w-full" :options="segOptions"
              @update:value="(v: any) => (proxyForm.contentType = v)" />
          </div>
          <div>
            <label class="fld-label">{{ t('audit.fUrgency') }}</label>
            <a-select :value="proxyForm.urgency" class="w-full" :options="urgencyOptions"
              @update:value="(v: any) => (proxyForm.urgency = v)" />
          </div>
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fIndustry') }}</label>
          <a-input v-model:value="proxyForm.industry" :maxlength="50" :placeholder="t('audit.fIndustryPlaceholder')" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fCategory') }}</label>
          <a-cascader v-model:value="proxyForm.category" :options="categoryOptions" multiple class="w-full"
            :placeholder="t('audit.fCategoryPlaceholder')" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fTags') }}</label>
          <a-select :value="proxyForm.tags" mode="tags" class="w-full" :placeholder="t('audit.fTagsPlaceholder')"
            @update:value="(v: any) => (proxyForm.tags = v)" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fContent') }} *</label>
          <RichEditor v-model="proxyForm.description" :placeholder="t('audit.fContentPlaceholder')" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fAttachment') }}</label>
          <QUpload manual multiple list-type="text" :max-count="5">
            <a-button><UploadOutlined /> {{ t('audit.fPickFile') }}</a-button>
          </QUpload>
          <div class="mt-1 text-[12px] text-[hsl(var(--secondary-text))]">{{ t('audit.fAttachmentHint') }}</div>
        </div>
        <div class="notify-box">
          <a-checkbox v-model:checked="proxyForm.notify">{{ t('audit.fNotify') }}</a-checkbox>
          <div class="ml-6 mt-1 text-[12px] text-[hsl(var(--secondary-text))]">{{ t('audit.fNotifyHint') }}</div>
        </div>
      </div>
    </a-modal>

    <!-- 编辑抽屉（全字段） -->
    <a-drawer v-model:open="editOpen" :title="t('audit.editTitle')" :width="680">
      <div v-if="editForm" class="form-grid">
        <div>
          <label class="fld-label">{{ t('audit.fTitle') }} *</label>
          <a-input v-model:value="editForm.title" :maxlength="100" show-count />
        </div>
        <div class="two-col">
          <div>
            <label class="fld-label">{{ t('audit.fType') }}</label>
            <a-select :value="editForm.contentType" class="w-full" :options="segOptions"
              @update:value="(v: any) => editForm && (editForm.contentType = v)" />
          </div>
          <div>
            <label class="fld-label">{{ t('audit.fPublisher') }}</label>
            <a-select :value="editForm.publisherName" class="w-full" :options="publisherOptions"
              @update:value="(v: any) => editForm && (editForm.publisherName = v)" />
          </div>
        </div>
        <div class="two-col">
          <div>
            <label class="fld-label">{{ t('common.status') }}</label>
            <a-select :value="editForm.status" class="w-full" :options="editStatusOptions"
              @update:value="(v: any) => editForm && (editForm.status = v)" />
          </div>
          <div>
            <label class="fld-label">{{ t('audit.fUrgency') }}</label>
            <a-select :value="editForm.urgency" class="w-full" :options="urgencyOptions"
              @update:value="(v: any) => editForm && (editForm.urgency = v)" />
          </div>
        </div>
        <div class="two-col">
          <div>
            <label class="fld-label">{{ t('common.publishedAt') }}</label>
            <a-date-picker v-model:value="editForm.publishedAt" show-time value-format="YYYY-MM-DD HH:mm:ss" class="w-full" />
          </div>
          <div class="flex items-end pb-1 gap-2">
            <label class="fld-label mb-0">{{ t('audit.pin') }}</label>
            <a-switch v-model:checked="editForm.isPinned" />
          </div>
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fIndustry') }}</label>
          <a-input v-model:value="editForm.industry" :maxlength="50" :placeholder="t('audit.fIndustryPlaceholder')" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fCategory') }}</label>
          <a-cascader v-model:value="editForm.categoryPath" :options="categoryOptions" multiple class="w-full"
            :placeholder="t('audit.fCategoryPlaceholder')" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fTags') }}</label>
          <a-select :value="editForm.tags" mode="tags" class="w-full"
            @update:value="(v: any) => editForm && (editForm.tags = v)" />
        </div>
        <div>
          <label class="fld-label">{{ t('audit.fContent') }}</label>
          <RichEditor v-model="editForm.description" :placeholder="t('audit.fContentPlaceholder')" />
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="editOpen = false">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" @click="saveEdit">{{ t('common.save') }}</a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, SortAscendingOutlined, SortDescendingOutlined, SaveOutlined,
  OrderedListOutlined, UserAddOutlined, UpOutlined, DownOutlined, PushpinFilled, UploadOutlined
} from '@ant-design/icons-vue'
import Empty from '@q-web-plugin/empty'
import RichEditor from '@/components/rich-editor/index.vue'
import QUpload from '@/components/q-upload/index.vue'
import { QBigTable } from '@/components/q-big-table'
import type { TableColumn, QBigTableExpose } from '@/components/q-big-table'
import { getAuditList } from '@/apis/audit/auditApi'
import type { AuditItem } from '@/apis/audit/types'

defineOptions({ name: 'OperationAudit' })
definePage({
  name: 'OperationAudit',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.audit.DEFAULT'
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const urgencyColor: Record<string, string> = { normal: 'default', urgent: 'orange', critical: 'red' }
const statusColor: Record<string, string> = {
  published: 'green', archived: 'red', Pending: 'orange', Collecting: 'blue', Adopted: 'green', Closed: 'default'
}

// 分类树（label 走 i18n，避免硬编码中文）
const CATEGORY_TREE = [
  { value: 'product-line', k: 'productLine', children: [
    { value: '5g', k: 'm5g' }, { value: '4g', k: 'm4g' }, { value: 'wifi', k: 'wifi' }, { value: 'nbiot', k: 'nbiot' }
  ] },
  { value: 'solution', k: 'solution', children: [
    { value: 'smart-city', k: 'smartCity' }, { value: 'vehicle', k: 'vehicle' }, { value: 'iiot', k: 'iiot' }
  ] },
  { value: 'case', k: 'case', children: [
    { value: 'carrier', k: 'carrier' }, { value: 'enterprise', k: 'enterprise' }
  ] }
]
const categoryOptions = computed(() =>
  CATEGORY_TREE.map((g) => ({
    value: g.value,
    label: t('audit.cat.' + g.k),
    children: g.children.map((c) => ({ value: c.value, label: t('audit.cat.' + c.k) }))
  }))
)

const segType = ref<string>('opportunity')
const segOptions = computed(() => [
  { label: t('dict.auditContentType.opportunity'), value: 'opportunity' },
  { label: t('dict.auditContentType.request'), value: 'request' }
])
const segLabel = computed(() => t('dict.auditContentType.' + segType.value))

const keyword = ref('')
const statusFilter = ref<string | undefined>(undefined)
const publisherFilter = ref<string | undefined>(undefined)
const dateRange = ref<string[]>([])
const sortField = ref<'time' | 'title'>('time')
const sortOrder = ref<'ascend' | 'descend'>('descend')
const sortDirty = ref(false)

// ============ 表格选择 ============
const tableRef = ref<QBigTableExpose>()
const selectedRows = ref<AuditItem[]>([])
function onCheckboxChange() {
  selectedRows.value = (tableRef.value?.getCheckboxRecords() ?? []) as AuditItem[]
}
function clearSelection() {
  tableRef.value?.clearCheckboxRow()
  selectedRows.value = []
}
const gridConfig = computed(() => ({
  rowClassName: ({ row }: any) => ((row as AuditItem).isPinned ? 'pinned-row' : '')
}))

const allItems = ref<AuditItem[]>([])

const opportunityStatus = ['published', 'archived']
const requestStatus = ['Pending', 'Collecting', 'Adopted', 'Closed']
const statusOptions = computed(() =>
  (segType.value === 'opportunity' ? opportunityStatus : requestStatus).map((s) => ({
    label: t('dict.auditStatus.' + s), value: s
  }))
)
const editStatusOptions = computed(() =>
  ((editForm.value?.contentType ?? 'opportunity') === 'opportunity' ? opportunityStatus : requestStatus).map((s) => ({
    label: t('dict.auditStatus.' + s), value: s
  }))
)
const urgencyOptions = computed(() => [
  { label: t('dict.urgency.normal'), value: 'normal' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.critical'), value: 'critical' }
])
const sortFieldOptions = computed(() => [
  { label: t('audit.byTime'), value: 'time' },
  { label: t('audit.byTitle'), value: 'title' }
])

const publisherOptions = computed(() => {
  const set = new Set<string>()
  allItems.value.forEach((i) => set.add(i.publisherName))
  return [...set].map((p) => ({ label: p, value: p }))
})
const publisherPickOptions = computed(() => publisherOptions.value)

function renderSort(row: AuditItem, rowIndex: number) {
  return (
    <div class="flex items-center justify-center gap-1">
      <span class="text-[12px] text-[hsl(var(--secondary-text))] w-[20px] text-center">{row.sortNo}</span>
      <div class="flex flex-col">
        <a-button type="text" size="small" class="sort-arrow" disabled={rowIndex === 0} onClick={() => rowSortUp(row)}>
          <UpOutlined />
        </a-button>
        <a-button type="text" size="small" class="sort-arrow" disabled={rowIndex === filteredData.value.length - 1} onClick={() => rowSortDown(row)}>
          <DownOutlined />
        </a-button>
      </div>
    </div>
  )
}
function renderTitle(row: AuditItem) {
  return (
    <span class="flex items-center gap-1">
      {row.isPinned ? <PushpinFilled class="text-[hsl(var(--error))]" /> : null}
      <a class="text-[hsl(var(--primary))]" onClick={() => handleView(row)}>{row.title}</a>
    </span>
  )
}
function renderRowAction(row: AuditItem) {
  return (
    <span class="flex gap-1 flex-wrap">
      <a-button type="link" size="small" onClick={() => handleView(row)}>{t('common.view')}</a-button>
      <a-button type="link" size="small" onClick={() => openEdit(row)}>{t('common.edit')}</a-button>
      <a-button type="link" size="small" onClick={() => togglePin(row)}>{row.isPinned ? t('audit.unpin') : t('audit.pin')}</a-button>
      {row.status === 'published'
        ? <a-button type="link" size="small" danger onClick={() => openOffline(row)}>{t('audit.archive')}</a-button>
        : null}
      <a-button type="link" size="small" danger onClick={() => handleDelete(row)}>{t('common.delete')}</a-button>
    </span>
  )
}

const columns = computed<TableColumn[]>(() => [
  { field: 'sort', title: t('audit.sortCol'), width: 100, align: 'center', slots: { default: ({ row, rowIndex }: any) => renderSort(row, rowIndex) } },
  { field: 'title', title: t('audit.title'), width: 280, slots: { default: ({ row }: any) => renderTitle(row) } },
  {
    field: 'contentType',
    title: t('audit.contentType'),
    width: 110,
    slots: { default: ({ row }: any) => <a-tag color={row.contentType === 'opportunity' ? 'blue' : 'purple'}>{t('dict.auditContentType.' + row.contentType)}</a-tag> }
  },
  {
    field: 'urgency',
    title: t('audit.fUrgency'),
    width: 90,
    slots: { default: ({ row }: any) => <a-tag color={urgencyColor[row.urgency] ?? 'default'}>{t('dict.urgency.' + row.urgency)}</a-tag> }
  },
  { field: 'publisherName', title: t('audit.publisher'), width: 110 },
  {
    field: 'status',
    title: t('common.status'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] ?? 'default'}>{t('dict.auditStatus.' + row.status)}</a-tag> }
  },
  { field: 'publishedAt', title: t('common.publishedAt'), width: 170, slots: { default: ({ row }: any) => <span>{row.publishedAt || '--'}</span> } },
  { field: 'action', title: t('common.action'), width: 260, fixed: 'right', slots: { default: ({ row }: any) => renderRowAction(row) } }
])

const filteredData = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  let list = allItems.value.filter((r) => r.contentType === segType.value)
  if (kw) list = list.filter((r) => r.title.toLowerCase().includes(kw))
  if (statusFilter.value) list = list.filter((r) => r.status === statusFilter.value)
  if (publisherFilter.value) list = list.filter((r) => r.publisherName === publisherFilter.value)
  const [start, end] = dateRange.value || []
  if (start && end) {
    list = list.filter((r) => {
      const d = (r.publishedAt || '').slice(0, 10)
      return d >= start && d <= end
    })
  }
  list = [...list].sort((a, b) => a.sortNo - b.sortNo)
  const pinned = list.filter((r) => r.isPinned)
  const unpinned = list.filter((r) => !r.isPinned)
  const cmp = (a: AuditItem, b: AuditItem) => {
    let v = 0
    if (sortField.value === 'time') v = (a.publishedAt || '').localeCompare(b.publishedAt || '')
    else v = a.title.localeCompare(b.title, 'zh')
    return sortOrder.value === 'descend' ? -v : v
  }
  pinned.sort(cmp)
  unpinned.sort(cmp)
  return [...pinned, ...unpinned]
})

function onSegChange() {
  statusFilter.value = undefined
  clearSelection()
}

// ============ 行内手动排序 ============
function swapSort(a: AuditItem, b: AuditItem) {
  const tmp = a.sortNo
  a.sortNo = b.sortNo
  b.sortNo = tmp
  sortDirty.value = true
}
function rowSortUp(record: AuditItem) {
  const group = allItems.value.filter((r) => r.contentType === segType.value).sort((a, b) => a.sortNo - b.sortNo)
  const idx = group.findIndex((r) => r.id === record.id)
  if (idx > 0) swapSort(record, group[idx - 1]!)
}
function rowSortDown(record: AuditItem) {
  const group = allItems.value.filter((r) => r.contentType === segType.value).sort((a, b) => a.sortNo - b.sortNo)
  const idx = group.findIndex((r) => r.id === record.id)
  if (idx >= 0 && idx < group.length - 1) swapSort(record, group[idx + 1]!)
}
function saveInlineSort() {
  sortDirty.value = false
  message.success(t('audit.sortSaved'))
}

// ============ 排序 Modal ============
const sortModalOpen = ref(false)
const sortList = ref<AuditItem[]>([])
function openSortModal() {
  sortList.value = allItems.value
    .filter((r) => r.contentType === segType.value)
    .sort((a, b) => a.sortNo - b.sortNo)
    .map((r) => ({ ...r }))
  sortModalOpen.value = true
}
function modalUp(i: number) {
  if (i <= 0) return
  const arr = sortList.value
  const cur = arr[i]!
  arr[i] = arr[i - 1]!
  arr[i - 1] = cur
}
function modalDown(i: number) {
  const arr = sortList.value
  if (i >= arr.length - 1) return
  const cur = arr[i]!
  arr[i] = arr[i + 1]!
  arr[i + 1] = cur
}
function saveSortModal() {
  const order = new Map<string, number>()
  sortList.value.forEach((item, i) => order.set(item.id, i + 1))
  allItems.value.forEach((r) => {
    const no = order.get(r.id)
    if (no !== undefined) r.sortNo = no
  })
  sortModalOpen.value = false
  message.success(t('audit.sortSaved'))
}

// ============ 下架 ============
const offlineOpen = ref(false)
const offlineReason = ref('')
const offlineTargets = ref<string[]>([])
function openOffline(record: AuditItem) {
  offlineTargets.value = [record.id]
  offlineReason.value = ''
  offlineOpen.value = true
}
function batchOffline() {
  if (selectedRows.value.length > 50) {
    message.warning(t('audit.batchOver'))
    return
  }
  const ids = selectedRows.value
    .filter((r) => r.status === 'published')
    .map((r) => r.id)
  if (!ids.length) {
    message.warning(t('audit.noArchivable'))
    return
  }
  offlineTargets.value = ids
  offlineReason.value = ''
  offlineOpen.value = true
}
function confirmOffline() {
  if (!offlineReason.value.trim()) {
    message.warning(t('audit.offlineReasonRequired'))
    return
  }
  const ids = offlineTargets.value
  allItems.value.forEach((r) => {
    if (ids.includes(r.id)) r.status = 'archived'
  })
  message.success(t('audit.offlineDone', { n: ids.length }))
  offlineOpen.value = false
  clearSelection()
}

// ============ 删除 ============
function handleDelete(record: AuditItem) {
  Modal.confirm({
    title: t('audit.deleteConfirm'),
    onOk: () => {
      allItems.value = allItems.value.filter((r) => r.id !== record.id)
      clearSelection()
      message.success(t('common.success'))
    }
  })
}
function batchDelete() {
  if (selectedRows.value.length > 50) {
    message.warning(t('audit.batchOver'))
    return
  }
  Modal.confirm({
    title: t('audit.batchDeleteConfirm', { n: selectedRows.value.length }),
    onOk: () => {
      const ids = new Set(selectedRows.value.map((r) => r.id))
      allItems.value = allItems.value.filter((r) => !ids.has(r.id))
      clearSelection()
      message.success(t('common.success'))
    }
  })
}

function togglePin(record: AuditItem) {
  record.isPinned = !record.isPinned
  message.success(record.isPinned ? t('audit.pinDone') : t('audit.unpinDone'))
}

// ============ 查看 / 编辑 ============
function targetPath(row: AuditItem, sub: 'detail' | 'form') {
  return row.contentType === 'request' ? `/requirement/${sub}` : `/opportunity/${sub}`
}
function handleView(record: AuditItem) {
  router.push({ path: targetPath(record, 'detail'), query: { id: record.id } })
}

const editOpen = ref(false)
const editForm = ref<AuditItem | null>(null)
function openEdit(record: AuditItem) {
  editForm.value = { ...record, tags: [...record.tags], categoryPath: record.categoryPath.map((p) => [...p]) }
  editOpen.value = true
}
function saveEdit() {
  const f = editForm.value
  if (!f) return
  if (!f.title.trim()) {
    message.warning(t('audit.titleRequired'))
    return
  }
  const idx = allItems.value.findIndex((r) => r.id === f.id)
  if (idx >= 0) allItems.value[idx] = { ...f }
  message.success(t('audit.editSaved'))
  editOpen.value = false
  editForm.value = null
}

// ============ 替发方案 ============
const proxyOpen = ref(false)
const proxyForm = reactive({
  publisher: '',
  title: '',
  contentType: 'opportunity' as string,
  urgency: 'normal',
  industry: '',
  category: [] as string[][],
  tags: [] as string[],
  description: '',
  notify: true
})
function resetProxy() {
  proxyForm.publisher = publisherOptions.value[0]?.value ?? ''
  proxyForm.title = ''
  proxyForm.contentType = 'opportunity'
  proxyForm.urgency = 'normal'
  proxyForm.industry = ''
  proxyForm.category = []
  proxyForm.tags = []
  proxyForm.description = ''
  proxyForm.notify = true
}
function openProxy() {
  resetProxy()
  proxyOpen.value = true
}
function submitProxy() {
  if (!proxyForm.title.trim()) {
    message.warning(t('audit.titleRequired'))
    return
  }
  if (!proxyForm.description.trim()) {
    message.warning(t('audit.contentRequired'))
    return
  }
  const maxSort = allItems.value.reduce((m, r) => Math.max(m, r.sortNo), 0)
  allItems.value.unshift({
    id: 'AUD' + Date.now(),
    title: proxyForm.title,
    contentType: proxyForm.contentType,
    publisherName: proxyForm.publisher,
    status: 'published',
    isPinned: false,
    publishedAt: new Date().toISOString().slice(0, 19).replace('T', ' '),
    sortNo: maxSort + 1,
    urgency: proxyForm.urgency,
    industry: proxyForm.industry,
    tags: [...proxyForm.tags],
    categoryPath: proxyForm.category.map((p) => [...p]),
    description: proxyForm.description
  })
  message.success(t('audit.proxyDone', { name: proxyForm.publisher }))
  if (proxyForm.notify) message.info(t('audit.proxyNotified'))
  proxyOpen.value = false
}

async function load() {
  const res = await getAuditList({ pageNumber: 1, pageSize: 999 })
  allItems.value = res.records
}
load()
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  padding: 14px 16px;
  border: 1px solid hsl(var(--line));
  border-radius: 12px;
  margin-bottom: 16px;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: hsl(var(--primary) / 0.06);
  border-radius: 8px;
  margin-top: 12px;
}
.sort-arrow {
  padding: 0;
  height: 16px;
  line-height: 16px;
}
.sort-list {
  max-height: 56vh;
  overflow-y: auto;
}
.sort-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-bottom: 1px solid hsl(var(--line));
}
.form-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.two-col {
  display: flex;
  gap: 12px;
}
.two-col > div {
  flex: 1;
}
.fld-label {
  display: block;
  margin-bottom: 4px;
  font-weight: 500;
  font-size: 13px;
}
.notify-box {
  padding: 10px 14px;
  background: hsl(var(--primary) / 0.06);
  border-radius: 8px;
}
:deep(.pinned-row .vxe-body--column) {
  background: hsl(var(--primary) / 0.06) !important;
}
</style>
