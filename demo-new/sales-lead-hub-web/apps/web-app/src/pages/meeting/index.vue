<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto flex flex-col">
    <!-- 标题 + 批量发布入口 -->
    <div class="flex items-center justify-between mb-2 flex-wrap gap-2">
      <h2 class="text-[18px] font-bold">{{ t('meeting.pageTitle') }}</h2>
    </div>
    <div class="mb-4 flex items-center gap-3 flex-wrap">
      <a-button type="primary" size="large" @click="goBatch">
        <template #icon><AppstoreAddOutlined /></template>
        {{ t('meeting.batchPublish') }}
      </a-button>
      <span class="text-[13px] text-[hsl(var(--secondary-text))]">{{ t('meeting.batchHint') }}</span>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <a-input-search v-model:value="keyword" :placeholder="t('meeting.searchPlaceholder')" allow-clear class="w-[220px]" />
      <a-select :value="statusFilter" class="w-[130px]" allow-clear :placeholder="t('common.status')"
        @update:value="(v: any) => (statusFilter = v ?? '')">
        <a-select-option v-for="o in statusOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
      </a-select>
      <a-select :value="priorityFilter" class="w-[120px]" allow-clear :placeholder="t('meeting.priority')"
        @update:value="(v: any) => (priorityFilter = v ?? '')">
        <a-select-option v-for="o in priorityOptions" :key="o.value" :value="o.value">{{ o.label }}</a-select-option>
      </a-select>
      <a-range-picker :value="(dateRange as any)" value-format="YYYY-MM-DD" class="w-[230px]"
        :placeholder="[t('meeting.meetingDateStart'), t('meeting.meetingDateEnd')]"
        @update:value="(v: any) => (dateRange = v || [])" />
      <a-select :value="assigneeFilter" mode="multiple" class="w-[220px]" allow-clear :max-tag-count="1"
        :placeholder="t('meeting.assigneeFilter')" :options="userOptions"
        @update:value="(v: any) => (assigneeFilter = v ?? [])" />
      <div class="ml-auto flex gap-2">
        <a-button @click="goBatch">
          <template #icon><AppstoreAddOutlined /></template>
          {{ t('meeting.batchPublishShort') }}
        </a-button>
        <a-button type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          {{ t('meeting.addTitle') }}
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
        :grid-config="gridConfig"
        :pagination-config="{ pageSize: 10 }"
        height="100%"
        @checkbox-change="onCheckboxChange"
      />
    </div>

    <!-- 批量催办栏 -->
    <div v-if="selectedRows.length" class="batch-bar">
      <span>{{ t('meeting.selectedN', { n: selectedRows.length }) }}</span>
      <a-button type="primary" size="small" @click="batchUrge">
        <template #icon><BellOutlined /></template>
        {{ t('meeting.batchUrge') }}
      </a-button>
      <a-button size="small" @click="clearSelection">{{ t('meeting.clearSelection') }}</a-button>
    </div>

    <!-- 新建/编辑 Drawer -->
    <a-drawer
      v-model:open="drawerOpen"
      :title="editId ? t('meeting.editTitle') : t('meeting.addTitle')"
      :width="520"
      @close="closeDrawer"
    >
      <QForm ref="qFormRef" :schemas="schemas" v-model:model="formModel" />
      <template #footer>
        <div class="flex justify-end gap-2">
          <a-button @click="closeDrawer">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" @click="handleSave">{{ t('common.save') }}</a-button>
        </div>
      </template>
    </a-drawer>

    <!-- 催办 Modal -->
    <a-modal v-model:open="urgeOpen" :title="t('meeting.urge')" :ok-text="t('meeting.urge')" @ok="confirmUrge">
      <div v-if="urgeTarget" class="urge-info">
        <div><span class="lbl">{{ t('meeting.taskId') }}：</span>{{ urgeTarget.id }}</div>
        <div><span class="lbl">{{ t('meeting.taskDesc') }}：</span>{{ urgeTarget.taskDesc }}</div>
        <div class="flex items-center gap-1">
          <span class="lbl">{{ t('common.status') }}：</span>
          <a-tag :color="statusColor[urgeTarget.status] || 'default'">{{ t('dict.taskStatus.' + urgeTarget.status) }}</a-tag>
        </div>
        <div class="flex items-center gap-1 flex-wrap">
          <span class="lbl">{{ t('meeting.assignees') }}：</span>
          <a-tag v-for="n in urgeTarget.assigneeNames" :key="n" color="blue">{{ n }}</a-tag>
        </div>
      </div>
      <a-textarea v-model:value="urgeRemark" class="mt-3" :rows="3" :placeholder="t('meeting.urgePlaceholder')" :maxlength="200" show-count />
    </a-modal>

    <!-- 作废 Modal -->
    <a-modal v-model:open="cancelOpen" :title="t('meeting.cancel')" :ok-text="t('meeting.cancel')" ok-type="danger" @ok="confirmCancel">
      <a-textarea v-model:value="cancelReason" :rows="4" :placeholder="t('meeting.cancelPlaceholder')" :maxlength="200" show-count />
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PlusOutlined, AppstoreAddOutlined, BellOutlined } from '@ant-design/icons-vue'
import { QForm } from '@/components/q-form'
import { QBigTable } from '@/components/q-big-table'
import type { TableColumn, QBigTableExpose } from '@/components/q-big-table'
import { getMeetingList, saveMeetingTask, urgeMeetingTask, cancelMeetingTask } from '@/apis/meeting/meetingApi'
import type { MeetingTaskItem } from '@/apis/meeting/types'

defineOptions({ name: 'MeetingTask' })
definePage({
  name: 'MeetingTask',
  meta: {
    layout: 'default',
    order: 2,
    menu: { icon: 'q-icon:calendar-linear' },
    title: 'meeting'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

// Mock 销售人员列表（执行人来源，非自由输入）
const SALES_USERS = [
  { name: '张伟', dept: '无线模组产品部' },
  { name: '李娜', dept: '行业解决方案部' },
  { name: '王强', dept: '车载产品部' },
  { name: '赵敏', dept: '无线模组产品部' },
  { name: '陈涛', dept: '短距产品部' },
  { name: '刘洋', dept: '天线产品部' },
  { name: '孙丽', dept: '行业解决方案部' },
  { name: '周杰', dept: '定位产品部' },
  { name: '郑浩', dept: '定位产品部' },
  { name: '吴敏', dept: '支付终端部' },
  { name: '冯雪', dept: '农业物联网部' }
]
const userOptions = computed(() => SALES_USERS.map((u) => ({ value: u.name, label: `${u.name} · ${u.dept}` })))

const statusColor: Record<string, string> = {
  pending: 'blue',
  processing: 'orange',
  completed: 'green',
  transferred: 'purple',
  cancelled: 'default'
}
const priorityColor: Record<string, string> = {
  normal: 'default',
  urgent: 'orange',
  critical: 'red'
}

const statusOptions = computed(() => [
  { label: t('dict.taskStatus.pending'), value: 'pending' },
  { label: t('dict.taskStatus.processing'), value: 'processing' },
  { label: t('dict.taskStatus.completed'), value: 'completed' },
  { label: t('dict.taskStatus.transferred'), value: 'transferred' },
  { label: t('dict.taskStatus.cancelled'), value: 'cancelled' }
])
const priorityOptions = computed(() => [
  { label: t('dict.urgency.critical'), value: 'critical' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.normal'), value: 'normal' }
])

const allItems = ref<MeetingTaskItem[]>([])
const keyword = ref('')
const statusFilter = ref('')
const priorityFilter = ref('')
const assigneeFilter = ref<string[]>([])
const dateRange = ref<string[]>([])

// ============ 表格选择 ============
const tableRef = ref<QBigTableExpose>()
const selectedRows = ref<MeetingTaskItem[]>([])
function onCheckboxChange() {
  selectedRows.value = (tableRef.value?.getCheckboxRecords() ?? []) as MeetingTaskItem[]
}
function clearSelection() {
  tableRef.value?.clearCheckboxRow()
  selectedRows.value = []
}

const gridConfig = computed(() => ({
  rowClassName: ({ row }: any) => (isOverdue(row as MeetingTaskItem) ? 'overdue-row' : '')
}))

function renderDeadline(row: MeetingTaskItem) {
  const overdue = isOverdue(row)
  return (
    <span class={overdue ? 'text-[hsl(var(--error))] font-medium' : ''}>
      {row.deadline || '--'}
      {overdue ? <span class="text-[hsl(var(--error))]"> · {t('meeting.overdue')}</span> : null}
    </span>
  )
}
function renderAction(row: MeetingTaskItem) {
  if (row.status === 'cancelled' || row.status === 'completed') {
    return <span class="text-[hsl(var(--secondary-text))]">--</span>
  }
  return (
    <span class="flex gap-1">
      <a-button type="link" size="small" onClick={() => openEdit(row)}>{t('common.edit')}</a-button>
      <a-button type="link" size="small" onClick={() => openUrge(row)}>{t('meeting.urge')}</a-button>
      {row.status !== 'transferred'
        ? <a-button type="link" size="small" danger onClick={() => openCancel(row)}>{t('meeting.cancel')}</a-button>
        : null}
    </span>
  )
}

const columns = computed<TableColumn[]>(() => [
  { field: 'id', title: t('meeting.taskId'), width: 110 },
  { field: 'meetingName', title: t('meeting.meetingName'), minWidth: 200 },
  { field: 'meetingDate', title: t('meeting.meetingDate'), width: 120 },
  { field: 'recorderName', title: t('meeting.recorder'), width: 100 },
  { field: 'taskDesc', title: t('meeting.taskDesc'), minWidth: 240 },
  {
    field: 'priority',
    title: t('meeting.priority'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={priorityColor[row.priority] || 'default'}>{t('dict.urgency.' + row.priority)}</a-tag> }
  },
  { field: 'deadline', title: t('meeting.deadline'), width: 180, slots: { default: ({ row }: any) => renderDeadline(row) } },
  {
    field: 'assignees',
    title: t('meeting.assignees'),
    minWidth: 160,
    slots: { default: ({ row }: any) => (row.assigneeNames || []).map((n: string) => <a-tag key={n} color="blue">{n}</a-tag>) }
  },
  {
    field: 'status',
    title: t('common.status'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] || 'default'}>{t('dict.taskStatus.' + row.status)}</a-tag> }
  },
  { field: 'action', title: t('common.action'), width: 190, fixed: 'right', slots: { default: ({ row }: any) => renderAction(row) } }
])

function isOverdue(row: MeetingTaskItem) {
  if (row.status === 'completed' || row.status === 'cancelled' || row.status === 'transferred') return false
  return new Date((row.deadline || '').replace(/-/g, '/')).getTime() < Date.now()
}

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  const [start, end] = dateRange.value || []
  return allItems.value.filter((i) => {
    if (kw && !(i.meetingName + i.taskDesc).toLowerCase().includes(kw)) return false
    if (statusFilter.value && i.status !== statusFilter.value) return false
    if (priorityFilter.value && i.priority !== priorityFilter.value) return false
    if (assigneeFilter.value.length && !assigneeFilter.value.some((a) => i.assigneeNames.includes(a))) return false
    if (start && end) {
      const d = (i.meetingDate || '').slice(0, 10)
      if (d < start || d > end) return false
    }
    return true
  })
})

async function load() {
  const res = await getMeetingList({ pageNumber: 1, pageSize: 999 })
  allItems.value = res.records
}

function goBatch() {
  router.push({ path: '/operation/batch' })
}

// ============ Drawer ============
const drawerOpen = ref(false)
const editId = ref<string>('')
const qFormRef = ref()
const formModel = reactive<{
  meetingName: string
  meetingDate: string
  recorderName: string
  taskDesc: string
  priority: string
  deadline: string
  assigneeNames: string[]
}>({
  meetingName: '',
  meetingDate: '',
  recorderName: '',
  taskDesc: '',
  priority: 'normal',
  deadline: '',
  assigneeNames: []
})

const schemas = computed(() => [
  {
    field: 'meetingName',
    label: t('meeting.meetingName'),
    component: 'Input',
    rules: [{ required: true, message: t('common.inputPlaceholder') }],
    componentProps: { placeholder: t('common.inputPlaceholder'), maxlength: 100 }
  },
  {
    field: 'meetingDate',
    label: t('meeting.meetingDate'),
    component: 'DatePicker',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), valueFormat: 'YYYY-MM-DD', style: 'width:100%' }
  },
  {
    field: 'recorderName',
    label: t('meeting.recorder'),
    component: 'Input',
    componentProps: { placeholder: t('common.inputPlaceholder'), maxlength: 50 }
  },
  {
    field: 'taskDesc',
    label: t('meeting.taskDesc'),
    component: 'Textarea',
    rules: [{ required: true, message: t('common.inputPlaceholder') }],
    componentProps: { placeholder: t('common.inputPlaceholder'), rows: 3, maxlength: 500, showCount: true }
  },
  {
    field: 'priority',
    label: t('meeting.priority'),
    component: 'Select',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), options: priorityOptions.value, style: 'width:100%' }
  },
  {
    field: 'deadline',
    label: t('meeting.deadline'),
    component: 'DatePicker',
    rules: [{ required: true, message: t('common.selectPlaceholder') }],
    componentProps: { placeholder: t('common.selectPlaceholder'), showTime: true, valueFormat: 'YYYY-MM-DD HH:mm:ss', style: 'width:100%' }
  },
  {
    field: 'assigneeNames',
    label: t('meeting.assignees'),
    component: 'Select',
    rules: [{ required: true, message: t('meeting.assigneesPlaceholder') }],
    componentProps: { placeholder: t('meeting.assigneesPlaceholder'), mode: 'multiple', options: userOptions.value, style: 'width:100%' }
  }
])

function resetForm() {
  formModel.meetingName = ''
  formModel.meetingDate = ''
  formModel.recorderName = ''
  formModel.taskDesc = ''
  formModel.priority = 'normal'
  formModel.deadline = ''
  formModel.assigneeNames = []
}
function openCreate() {
  editId.value = ''
  resetForm()
  drawerOpen.value = true
}
function openEdit(row: MeetingTaskItem) {
  editId.value = row.id
  formModel.meetingName = row.meetingName
  formModel.meetingDate = row.meetingDate
  formModel.recorderName = row.recorderName
  formModel.taskDesc = row.taskDesc
  formModel.priority = row.priority
  formModel.deadline = row.deadline
  formModel.assigneeNames = [...row.assigneeNames]
  drawerOpen.value = true
}
function closeDrawer() {
  drawerOpen.value = false
}
async function handleSave() {
  await qFormRef.value?.validate()
  if (editId.value) {
    const c = allItems.value.find((i) => i.id === editId.value)
    if (c) Object.assign(c, { ...formModel, assigneeNames: [...formModel.assigneeNames] })
  } else {
    allItems.value.unshift({
      id: 'MTK-' + Date.now(),
      meetingName: formModel.meetingName,
      meetingDate: formModel.meetingDate,
      recorderName: formModel.recorderName,
      taskDesc: formModel.taskDesc,
      priority: formModel.priority,
      deadline: formModel.deadline,
      assigneeNames: [...formModel.assigneeNames],
      status: 'pending',
      createdAt: new Date().toISOString().slice(0, 19).replace('T', ' ')
    })
  }
  allItems.value = [...allItems.value]
  saveMeetingTask({ id: editId.value || undefined, ...formModel }).catch(() => {})
  message.success(t('common.success'))
  drawerOpen.value = false
}

// ============ 催办 Modal ============
const urgeOpen = ref(false)
const urgeRemark = ref('')
const urgeTarget = ref<MeetingTaskItem | null>(null)
function openUrge(row: MeetingTaskItem) {
  urgeTarget.value = row
  urgeRemark.value = ''
  urgeOpen.value = true
}
function confirmUrge() {
  if (urgeTarget.value) urgeMeetingTask(urgeTarget.value.id, urgeRemark.value).catch(() => {})
  message.success(t('meeting.urgeSuccess'))
  urgeOpen.value = false
}
function batchUrge() {
  const targets = selectedRows.value.filter(
    (i) => i.status !== 'completed' && i.status !== 'cancelled'
  )
  if (!targets.length) {
    message.info(t('meeting.batchUrgeNone'))
    return
  }
  const names = new Set<string>()
  targets.forEach((tk) => tk.assigneeNames.forEach((n) => names.add(n)))
  message.success(t('meeting.batchUrgeSuccess', { n: names.size }))
  clearSelection()
}

// ============ 作废 Modal ============
const cancelOpen = ref(false)
const cancelReason = ref('')
const cancelTarget = ref<MeetingTaskItem | null>(null)
function openCancel(row: MeetingTaskItem) {
  cancelTarget.value = row
  cancelReason.value = ''
  cancelOpen.value = true
}
function confirmCancel() {
  if (cancelTarget.value) {
    cancelTarget.value.status = 'cancelled'
    allItems.value = [...allItems.value]
    cancelMeetingTask(cancelTarget.value.id, cancelReason.value).catch(() => {})
  }
  message.success(t('common.success'))
  cancelOpen.value = false
}

onMounted(load)
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin-top: 12px;
  background: hsl(var(--primary) / 0.06);
  border: 1px solid hsl(var(--primary) / 0.2);
  border-radius: 8px;
  font-size: 13px;
}
.urge-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  padding: 12px;
  background: hsl(var(--card-bg));
  border-radius: 8px;
}
.urge-info .lbl {
  color: hsl(var(--secondary-text));
}
:deep(.overdue-row .vxe-body--column) {
  background: hsl(var(--error) / 0.05) !important;
}
</style>
