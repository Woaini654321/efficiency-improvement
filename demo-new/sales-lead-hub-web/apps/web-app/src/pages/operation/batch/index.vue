<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <!-- 头部：标题 + 任务列表/单个新建 -->
    <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
      <div>
        <h2 class="text-[18px] font-bold">{{ t('batch.title') }}</h2>
        <div class="text-[13px] text-[hsl(var(--secondary-text))]">{{ t('batch.subtitle') }}</div>
      </div>
      <div class="flex gap-2">
        <a-button @click="goMeeting">
          <template #icon><UnorderedListOutlined /></template>
          {{ t('batch.taskList') }}
        </a-button>
        <a-button type="primary" @click="goMeeting">
          <template #icon><PlusOutlined /></template>
          {{ t('batch.singleCreate') }}
        </a-button>
      </div>
    </div>

    <a-steps :current="step" class="mb-6" size="small">
      <a-step :title="t('batch.step1')" />
      <a-step :title="t('batch.step2')" />
      <a-step :title="t('batch.step3')" />
    </a-steps>

    <!-- Step 1：会议信息 -->
    <div v-show="step === 0" class="max-w-[640px]">
      <a-segmented v-model:value="meetingSource" :options="sourceOptions" class="mb-4" />

      <div>
        <template v-if="meetingSource === 'exist'">
          <div class="mb-4">
            <div class="mb-[6px]">{{ t('batch.meeting') }}</div>
            <a-select
              :value="form.meetingId"
              :options="meetingOptions"
              :placeholder="t('common.selectPlaceholder')"
              class="w-full"
              show-search
              option-filter-prop="label"
              @update:value="(v: any) => (form.meetingId = v)"
              @change="(v: any) => onMeetingChange(v)"
            />
          </div>
          <div class="mb-4">
            <div class="mb-[6px]">{{ t('batch.meetingDate') }}</div>
            <a-input :value="form.meetingDate" disabled />
          </div>
        </template>
        <template v-else>
          <div class="mb-4">
            <div class="mb-[6px]">{{ t('batch.meetingName') }}</div>
            <a-input v-model:value="form.meetingName" :placeholder="t('common.inputPlaceholder')" :maxlength="80" />
          </div>
          <div class="mb-4">
            <div class="mb-[6px]">{{ t('batch.meetingDate') }}</div>
            <a-date-picker v-model:value="form.meetingDate" value-format="YYYY-MM-DD" class="w-full" />
          </div>
        </template>
        <div class="mb-4">
          <div class="mb-[6px]">{{ t('batch.recorder') }}</div>
          <a-input v-model:value="form.recorderName" :placeholder="t('common.inputPlaceholder')" :maxlength="20" />
        </div>
      </div>

      <div class="flex justify-end pt-4 border-t border-[hsl(var(--line))]">
        <a-button type="primary" @click="goStep2">{{ t('batch.next') }}</a-button>
      </div>
    </div>

    <!-- Step 2：填写任务 -->
    <div v-show="step === 1">
      <!-- 批量设置 -->
      <div class="bulk-bar mb-3">
        <span class="font-medium">{{ t('batch.bulkSet') }}：</span>
        <a-select :value="bulk.priority" :options="priorityOptions" :placeholder="t('batch.priority')" class="w-[140px]" allow-clear @update:value="(v: any) => (bulk.priority = v)" />
        <a-button size="small" @click="applyBulk('priority')">{{ t('batch.apply') }}</a-button>
        <a-date-picker v-model:value="bulk.deadline" value-format="YYYY-MM-DD" :placeholder="t('batch.deadline')" class="w-[160px]" />
        <a-button size="small" @click="applyBulk('deadline')">{{ t('batch.apply') }}</a-button>
        <a-select
          :value="bulk.executorIds"
          mode="multiple"
          :options="executorOptions"
          :placeholder="t('batch.executor')"
          class="w-[220px]"
          allow-clear
          :max-tag-count="1"
          @update:value="(v: any) => (bulk.executorIds = v)"
        />
        <a-button size="small" @click="applyBulk('executor')">{{ t('batch.apply') }}</a-button>
      </div>

      <!-- 任务行 -->
      <div v-for="(task, idx) in tasks" :key="task.key" class="task-row">
        <div class="flex items-center justify-between mb-2">
          <span class="font-medium">{{ t('batch.taskNo', { n: idx + 1 }) }}</span>
          <span class="flex gap-1">
            <a-button type="link" size="small" @click="copyTask(idx)">{{ t('batch.copyRow') }}</a-button>
            <a-button type="link" size="small" danger :disabled="tasks.length <= 1" @click="removeTask(idx)">{{ t('common.remove') }}</a-button>
          </span>
        </div>
        <a-row :gutter="12">
          <a-col :span="24" class="mb-2">
            <a-textarea v-model:value="task.desc" :rows="2" :maxlength="300" show-count :placeholder="t('batch.descPlaceholder')" />
          </a-col>
          <a-col :xs="24" :sm="6" class="mb-2">
            <a-select :value="task.priority" :options="priorityOptions" :placeholder="t('batch.priority')" class="w-full" @update:value="(v: any) => (task.priority = v)" />
          </a-col>
          <a-col :xs="24" :sm="6" class="mb-2">
            <a-date-picker v-model:value="task.deadline" value-format="YYYY-MM-DD" :placeholder="t('batch.deadline')" class="w-full" />
          </a-col>
          <a-col :xs="24" :sm="12" class="mb-2">
            <a-select :value="task.executorIds" mode="multiple" :options="executorOptions" :placeholder="t('batch.executor')" class="w-full" :max-tag-count="2" @update:value="(v: any) => (task.executorIds = v)" />
          </a-col>
        </a-row>
      </div>

      <div class="flex gap-3 mb-4">
        <a-button type="dashed" class="flex-1" @click="addTask">+ {{ t('batch.addTask') }}</a-button>
        <a-button type="dashed" @click="importModalOpen = true">
          <template #icon><ImportOutlined /></template>
          {{ t('batch.importFromFile') }}
        </a-button>
      </div>

      <!-- 发布预览 -->
      <h3 class="text-[15px] font-semibold mb-2">{{ t('batch.preview') }}</h3>
      <QBigTable :columns="previewColumns" :data="previewData" height="300" />

      <div class="flex justify-between pt-4 border-t border-[hsl(var(--line))] mt-4">
        <a-button @click="step = 0">{{ t('batch.prev') }}</a-button>
        <span class="flex gap-2">
          <a-button @click="resetTasks">{{ t('common.reset') }}</a-button>
          <a-button type="primary" :disabled="!validCount" @click="openConfirm">
            {{ t('batch.publishN', { n: validCount }) }}
          </a-button>
        </span>
      </div>
    </div>

    <!-- Step 3：发布完成 -->
    <div v-show="step === 2">
      <a-result status="success" :title="t('batch.successTitle')" :sub-title="t('batch.successSub', { n: resultIds.length })">
        <template #extra>
          <div class="flex flex-wrap gap-2 justify-center mb-4">
            <a-tag v-for="id in resultIds" :key="id" color="blue">{{ id }}</a-tag>
          </div>
          <div class="flex gap-2 justify-center">
            <a-button @click="restart">{{ t('batch.continue') }}</a-button>
            <a-button type="primary" @click="goMeeting">{{ t('batch.taskList') }}</a-button>
          </div>
        </template>
      </a-result>
    </div>

    <!-- 发布确认 Modal -->
    <a-modal
      v-model:open="confirmOpen"
      :title="t('batch.confirmTitle')"
      :width="560"
      :ok-text="t('batch.confirmOk')"
      :confirm-loading="publishing"
      @ok="handlePublish"
    >
      <a-alert type="info" show-icon :message="t('batch.confirmAlert')" class="mb-3" />
      <div class="mb-3 text-[13px]">
        <div><span class="font-medium">{{ t('batch.meetingName') }}：</span>{{ meetingDisplayName }}</div>
        <div><span class="font-medium">{{ t('batch.meetingDate') }}：</span>{{ form.meetingDate || '--' }}</div>
        <div class="flex items-center gap-1"><span class="font-medium">{{ t('batch.taskCount') }}：</span><a-tag color="blue">{{ validCount }}</a-tag></div>
      </div>
      <QBigTable :columns="previewColumns" :data="previewData" height="240" />
    </a-modal>

    <!-- 从文件导入 Modal -->
    <a-modal
      v-model:open="importModalOpen"
      :title="t('batch.importTitle')"
      :width="640"
      :ok-text="t('batch.importConfirm', { n: importedTasks.length })"
      :ok-button-props="{ disabled: !importedTasks.length }"
      @ok="confirmImport"
      @cancel="resetImport"
    >
      <a-alert type="info" show-icon class="mb-3"
        :message="t('batch.importAlertTitle')" :description="t('batch.importAlertDesc')" />
      <div class="flex gap-3 mb-3">
        <input ref="csvInputRef" type="file" accept=".csv,.txt" style="display: none" @change="onCsvInputChange" />
        <a-button type="primary" ghost @click="csvInputRef?.click()">
          <template #icon><UploadOutlined /></template>
          {{ t('batch.uploadCsv') }}
        </a-button>
        <a-button @click="downloadTemplate">
          <template #icon><DownloadOutlined /></template>
          {{ t('batch.downloadTemplate') }}
        </a-button>
      </div>
      <div v-if="importFileName" class="import-file mb-3">
        <FileTextOutlined /> {{ t('batch.selectedFile') }}：{{ importFileName }}
        <a-button type="link" size="small" @click="resetImport">{{ t('common.close') }}</a-button>
      </div>
      <div v-if="importedTasks.length">
        <div class="text-[13px] font-medium mb-2">{{ t('batch.importPreview', { n: importedTasks.length }) }}</div>
        <QBigTable :columns="previewColumns" :data="importPreviewData" height="240" />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, UnorderedListOutlined, ImportOutlined, UploadOutlined, DownloadOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import { QBigTable } from '@/components/q-big-table'
import type { TableColumn } from '@/components/q-big-table'
import { getBatchMeta, publishBatchTasks } from '@/apis/batch/batchApi'
import type { BatchMeeting, BatchExecutor } from '@/apis/batch/types'

defineOptions({ name: 'OperationBatch' })
definePage({
  name: 'OperationBatch',
  meta: {
    layout: false,
    menu: true,
    title: 'operation.batch.DEFAULT'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()
const router = useRouter()

const step = ref(0)
const publishing = ref(false)

function goMeeting() {
  router.push({ path: '/meeting' })
}

// ============ 元数据 ============
const meetings = ref<BatchMeeting[]>([])
const executors = ref<BatchExecutor[]>([])
const meetingOptions = computed(() => meetings.value.map((m) => ({ label: `${m.name}（${m.meetingDate}）`, value: m.id })))
const executorOptions = computed(() => executors.value.map((e) => ({ label: `${e.name} · ${e.deptName}`, value: e.id })))

// 优先级统一使用 dict.urgency（特急/紧急/普通）
const priorityColor: Record<string, string> = { normal: 'default', urgent: 'orange', critical: 'red' }
const priorityOptions = computed(() => [
  { label: t('dict.urgency.critical'), value: 'critical' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.normal'), value: 'normal' }
])
const sourceOptions = computed(() => [
  { label: t('batch.sourceExist'), value: 'exist' },
  { label: t('batch.sourceNew'), value: 'new' }
])

// ============ Step1 表单 ============
const meetingSource = ref<'exist' | 'new'>('exist')
const form = reactive<{ meetingId?: string | undefined; meetingName: string; meetingDate: string; recorderName: string }>({
  meetingId: undefined,
  meetingName: '',
  meetingDate: '',
  recorderName: ''
})
function onMeetingChange(id: string) {
  const m = meetings.value.find((x) => x.id === id)
  if (m) {
    form.meetingDate = m.meetingDate
    if (!form.recorderName) form.recorderName = m.recorderName
  }
}
const meetingDisplayName = computed(() => {
  if (meetingSource.value === 'new') return form.meetingName
  return meetings.value.find((m) => m.id === form.meetingId)?.name ?? ''
})

// ============ Step2 任务行 ============
interface TaskRow {
  key: number
  desc: string
  priority: string
  deadline: string
  executorIds: string[]
}
let taskSeq = 0
const tasks = reactive<TaskRow[]>([])
function blankTask(): TaskRow {
  return { key: ++taskSeq, desc: '', priority: 'normal', deadline: '', executorIds: [] }
}
function addTask() {
  tasks.push(blankTask())
}
function removeTask(idx: number) {
  tasks.splice(idx, 1)
}
function copyTask(idx: number) {
  const src = tasks[idx]
  if (!src) return
  tasks.splice(idx + 1, 0, { ...src, key: ++taskSeq, executorIds: [...src.executorIds] })
}
function resetTasks() {
  tasks.splice(0, tasks.length)
  tasks.push(blankTask())
}

// 批量设置
const bulk = reactive<{ priority?: string | undefined; deadline: string; executorIds: string[] }>({
  priority: undefined,
  deadline: '',
  executorIds: []
})
function applyBulk(kind: 'priority' | 'deadline' | 'executor') {
  tasks.forEach((task) => {
    if (kind === 'priority' && bulk.priority) task.priority = bulk.priority
    if (kind === 'deadline' && bulk.deadline) task.deadline = bulk.deadline
    if (kind === 'executor' && bulk.executorIds.length) task.executorIds = [...bulk.executorIds]
  })
  message.success(t('batch.applied'))
}

// 预览
const executorName = (id: string) => executors.value.find((e) => e.id === id)?.name ?? id
const validRows = computed(() => tasks.filter((task) => task.desc.trim()))
const validCount = computed(() => validRows.value.length)
function toPreview(rows: TaskRow[]) {
  return rows.map((task, i) => ({
    no: i + 1,
    desc: task.desc,
    priority: task.priority,
    deadline: task.deadline,
    executorNames: task.executorIds.map(executorName).join('、')
  }))
}
const previewData = computed(() => toPreview(validRows.value))
const previewColumns: TableColumn[] = [
  { field: 'no', title: t('batch.col.no'), width: 60 },
  { field: 'desc', title: t('batch.col.desc'), minWidth: 240, slots: { default: ({ row }: any) => <span>{row.desc || '--'}</span> } },
  {
    field: 'priority',
    title: t('batch.col.priority'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={priorityColor[row.priority] || 'default'}>{t('dict.urgency.' + row.priority)}</a-tag> }
  },
  { field: 'deadline', title: t('batch.col.deadline'), width: 130, slots: { default: ({ row }: any) => <span>{row.deadline || '--'}</span> } },
  { field: 'executorNames', title: t('batch.col.executor'), minWidth: 160, slots: { default: ({ row }: any) => <span>{row.executorNames || '--'}</span> } }
]

// ============ 步骤流转 ============
function goStep2() {
  if (meetingSource.value === 'exist' && !form.meetingId) {
    message.warning(t('batch.meetingRequired'))
    return
  }
  if (meetingSource.value === 'new' && !form.meetingName) {
    message.warning(t('batch.meetingNameRequired'))
    return
  }
  if (!form.meetingDate) {
    message.warning(t('batch.meetingDateRequired'))
    return
  }
  step.value = 1
}

// 逐行校验：截止日期 + 执行人非空
function validateRows(): boolean {
  const rows = validRows.value
  if (!rows.length) {
    message.warning(t('batch.taskRequired'))
    return false
  }
  for (let i = 0; i < rows.length; i++) {
    const r = rows[i]!
    if (!r.deadline) {
      message.warning(t('batch.rowDeadlineRequired', { n: i + 1 }))
      return false
    }
    if (!r.executorIds.length) {
      message.warning(t('batch.rowExecutorRequired', { n: i + 1 }))
      return false
    }
  }
  return true
}

const confirmOpen = ref(false)
function openConfirm() {
  if (!validateRows()) return
  confirmOpen.value = true
}

const resultIds = ref<string[]>([])
async function handlePublish() {
  publishing.value = true
  try {
    await publishBatchTasks({
      meetingSource: meetingSource.value,
      meetingId: form.meetingId,
      meetingName: form.meetingName,
      meetingDate: form.meetingDate,
      recorderName: form.recorderName,
      tasks: validRows.value.map((task) => ({
        desc: task.desc,
        priority: task.priority,
        deadline: task.deadline,
        executorIds: task.executorIds
      }))
    }).catch(() => {})
    resultIds.value = validRows.value.map((_, i) => `TASK-${Date.now()}-${i + 1}`)
    confirmOpen.value = false
    step.value = 2
    message.success(t('batch.publishedN', { n: resultIds.value.length }))
  } finally {
    publishing.value = false
  }
}

function restart() {
  step.value = 0
  meetingSource.value = 'exist'
  form.meetingId = undefined
  form.meetingName = ''
  form.meetingDate = ''
  form.recorderName = ''
  resultIds.value = []
  resetTasks()
}

// ============ 从文件导入（展示层）============
const importModalOpen = ref(false)
const importedTasks = ref<TaskRow[]>([])
const importFileName = ref('')
const importPreviewData = computed(() => toPreview(importedTasks.value))

function parseCSV(text: string): TaskRow[] {
  const lines = text.split(/\r?\n/).filter((l) => l.trim())
  if (lines.length < 2) {
    message.warning(t('batch.importEmpty'))
    return []
  }
  const header = lines[0]!.split(',').map((h) => h.trim().toLowerCase())
  const rows: TaskRow[] = []
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i]!.split(',').map((c) => c.trim().replace(/^"(.*)"$/, '$1'))
    if (!cols[0]) continue
    const row: TaskRow = { key: ++taskSeq, desc: '', priority: 'normal', deadline: '', executorIds: [] }
    header.forEach((h, j) => {
      const val = cols[j] || ''
      if (h.includes('描述') || h.includes('任务') || h === 'description') {
        row.desc = val || row.desc
      } else if (h.includes('优先级') || h === 'priority') {
        if (val.includes('特急') || val === 'critical') row.priority = 'critical'
        else if (val.includes('紧急') || val === 'urgent') row.priority = 'urgent'
        else row.priority = 'normal'
      } else if (h.includes('截止') || h.includes('日期') || h === 'deadline') {
        row.deadline = val
      } else if (h.includes('执行') || h.includes('负责人') || h === 'assignee') {
        row.executorIds = val
          .split(/[;；、]/)
          .map((n) => executors.value.find((e) => e.name === n.trim())?.id)
          .filter((x): x is string => !!x)
      }
    })
    if (!row.desc && cols[0]) row.desc = cols[0]
    if (row.desc) rows.push(row)
  }
  return rows
}
const csvInputRef = ref<HTMLInputElement>()
function onCsvInputChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) handleFileImport(file)
  input.value = ''
}
function handleFileImport(file: File) {
  importFileName.value = file.name
  const reader = new FileReader()
  reader.onload = (e) => {
    const text = String(e.target?.result || '')
    const parsed = parseCSV(text)
    importedTasks.value = parsed
  }
  reader.readAsText(file, 'UTF-8')
  return false
}
function downloadTemplate() {
  const header = t('batch.tplHeader')
  const examples = t('batch.tplExamples')
  const csv = '﻿' + header + '\n' + examples.split('|').join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = t('batch.tplFileName')
  a.click()
  URL.revokeObjectURL(url)
  message.success(t('batch.tplDownloaded'))
}
function confirmImport() {
  const existing = tasks.filter((r) => r.desc.trim())
  tasks.splice(0, tasks.length, ...existing, ...importedTasks.value)
  if (!tasks.length) tasks.push(blankTask())
  message.success(t('batch.imported', { n: importedTasks.value.length }))
  importModalOpen.value = false
  resetImport()
}
function resetImport() {
  importedTasks.value = []
  importFileName.value = ''
}

onMounted(async () => {
  const meta = await getBatchMeta()
  meetings.value = meta.meetings
  executors.value = meta.executors
  resetTasks()
})
</script>

<style scoped>
.bulk-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  background: hsl(var(--card-bg));
}

.task-row {
  padding: 12px;
  border: 1px solid hsl(var(--line));
  border-radius: 8px;
  margin-bottom: 12px;
}
.import-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: hsl(var(--primary) / 0.06);
  border: 1px solid hsl(var(--primary) / 0.2);
  border-radius: 6px;
  font-size: 13px;
}
</style>
