<template>
  <div class="h-full p-[16px] bg-white rounded">
    <QBigTable
      ref="tableRef"
      :search-config="searchConfig"
      :toolbar-config="toolbarConfig"
      :columns="columns"
      :query-api="queryApi"
      height="100%"
    />

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
    <a-modal
      v-model:open="urgeOpen"
      :title="t('meeting.urge')"
      :ok-text="t('meeting.urge')"
      @ok="confirmUrge"
    >
      <a-textarea v-model:value="urgeRemark" :rows="4" :placeholder="t('meeting.urgePlaceholder')" :maxlength="200" show-count />
    </a-modal>

    <!-- 作废 Modal -->
    <a-modal
      v-model:open="cancelOpen"
      :title="t('meeting.cancel')"
      :ok-text="t('meeting.cancel')"
      ok-type="danger"
      @ok="confirmCancel"
    >
      <a-textarea v-model:value="cancelReason" :rows="4" :placeholder="t('meeting.cancelPlaceholder')" :maxlength="200" show-count />
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { QBigTable } from '@/components/q-big-table'
import type { QBigTableExpose, ToolbarButton, TableColumn } from '@/components/q-big-table'
import type { FormSchema } from '@/components/q-form'
import { QForm } from '@/components/q-form'
import { getMeetingList, saveMeetingTask, urgeMeetingTask, cancelMeetingTask } from '@/apis/meeting/meetingApi'
import type { MeetingTaskItem } from '@/apis/meeting/types'

defineOptions({ name: 'MeetingTask' })
definePage({
  name: 'MeetingTask',
  meta: {
    layout: 'default',
    menu: { icon: 'q-icon:calendar-linear' },
    title: 'meeting'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const tableRef = ref<QBigTableExpose | null>(null)

const statusColor: Record<string, string> = {
  pending: 'blue',
  processing: 'orange',
  done: 'green',
  transferred: 'default',
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
  { label: t('dict.taskStatus.done'), value: 'done' },
  { label: t('dict.taskStatus.transferred'), value: 'transferred' },
  { label: t('dict.taskStatus.cancelled'), value: 'cancelled' }
])
const priorityOptions = computed(() => [
  { label: t('dict.urgency.normal'), value: 'normal' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.critical'), value: 'critical' }
])

const searchConfig: FormSchema[] = [
  {
    field: 'keyword',
    label: t('common.keyword'),
    component: 'Input',
    componentProps: { placeholder: t('meeting.searchPlaceholder'), allowClear: true, maxlength: 200 }
  },
  {
    field: 'status',
    label: t('common.status'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:150px', options: statusOptions.value }
  },
  {
    field: 'priority',
    label: t('meeting.priority'),
    component: 'Select',
    componentProps: { placeholder: t('common.selectPlaceholder'), allowClear: true, style: 'width:140px', options: priorityOptions.value }
  }
]

const toolbarConfig: ToolbarButton[] = [
  { label: t('meeting.addTitle'), type: 'primary', onClick: () => openCreate() }
]

function isOverdue(row: MeetingTaskItem) {
  if (row.status === 'done' || row.status === 'cancelled') return false
  return new Date(row.deadline).getTime() < Date.now()
}

const columns: TableColumn[] = [
  { field: 'id', title: t('meeting.taskId'), width: 110 },
  { field: 'meetingName', title: t('meeting.meetingName'), minWidth: 200 },
  { field: 'meetingDate', title: t('meeting.meetingDate'), width: 120 },
  { field: 'recorderName', title: t('meeting.recorder'), width: 100 },
  { field: 'taskDesc', title: t('meeting.taskDesc'), minWidth: 240, showOverflow: true },
  {
    field: 'priority',
    title: t('meeting.priority'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={priorityColor[row.priority] ?? 'default'}>{t('dict.urgency.' + row.priority)}</a-tag> }
  },
  {
    field: 'deadline',
    title: t('meeting.deadline'),
    width: 170,
    slots: {
      default: ({ row }: any) => (
        <span class={isOverdue(row) ? 'text-[hsl(var(--error))] font-medium' : ''}>{row.deadline ?? '--'}</span>
      )
    }
  },
  {
    field: 'assigneeNames',
    title: t('meeting.assignees'),
    minWidth: 160,
    slots: { default: ({ row }: any) => (row.assigneeNames ?? []).map((n: string) => <a-tag key={n}>{n}</a-tag>) }
  },
  {
    field: 'status',
    title: t('common.status'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={statusColor[row.status] ?? 'default'}>{t('dict.taskStatus.' + row.status)}</a-tag> }
  },
  {
    title: t('common.action'),
    width: 190,
    fixed: 'right',
    slots: {
      default: ({ row }: any) => (
        <span class="flex gap-1">
          <a-button type="link" onClick={() => openEdit(row)}>{t('common.edit')}</a-button>
          <a-button type="link" onClick={() => openUrge(row)}>{t('meeting.urge')}</a-button>
          {row.status !== 'cancelled' && row.status !== 'done' ? (
            <a-button type="link" danger onClick={() => openCancel(row)}>{t('meeting.cancel')}</a-button>
          ) : null}
        </span>
      )
    }
  }
]

async function queryApi({ page }: any, searchParams: any) {
  const result = await getMeetingList({
    ...searchParams,
    pageNumber: page.pageNumber,
    pageSize: page.pageSize
  })
  return { result: result.records, page: { total: result.total } }
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
    componentProps: { placeholder: t('meeting.assigneesPlaceholder'), mode: 'tags', style: 'width:100%' }
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
  await saveMeetingTask({ id: editId.value || undefined, ...formModel })
  message.success(t('common.success'))
  drawerOpen.value = false
  tableRef.value?.refresh()
}

// ============ 催办 Modal ============
const urgeOpen = ref(false)
const urgeRemark = ref('')
const urgeId = ref('')
function openUrge(row: MeetingTaskItem) {
  urgeId.value = row.id
  urgeRemark.value = ''
  urgeOpen.value = true
}
async function confirmUrge() {
  await urgeMeetingTask(urgeId.value, urgeRemark.value)
  message.success(t('meeting.urgeSuccess'))
  urgeOpen.value = false
  tableRef.value?.refresh()
}

// ============ 作废 Modal ============
const cancelOpen = ref(false)
const cancelReason = ref('')
const cancelId = ref('')
function openCancel(row: MeetingTaskItem) {
  cancelId.value = row.id
  cancelReason.value = ''
  cancelOpen.value = true
}
async function confirmCancel() {
  await cancelMeetingTask(cancelId.value, cancelReason.value)
  message.success(t('common.success'))
  cancelOpen.value = false
  tableRef.value?.refresh()
}
</script>
