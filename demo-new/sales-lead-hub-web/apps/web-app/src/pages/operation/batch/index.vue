<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto">
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-[18px] font-bold">{{ t('batch.title') }}</h2>
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
              @update:value="(v: any) => (form.meetingId = v)"
              @change="onMeetingChange"
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
            <a-select :value="task.executorIds" mode="multiple" :options="executorOptions" :placeholder="t('batch.executor')" class="w-full" @update:value="(v: any) => (task.executorIds = v)" />
          </a-col>
        </a-row>
      </div>

      <a-button type="dashed" block class="mb-4" @click="addTask">+ {{ t('batch.addTask') }}</a-button>

      <!-- 发布预览 -->
      <h3 class="text-[15px] font-semibold mb-2">{{ t('batch.preview') }}</h3>
      <QBigTable :columns="previewColumns" :data="previewData" height="300" />

      <div class="flex justify-between pt-4 border-t border-[hsl(var(--line))] mt-4">
        <a-button @click="step = 0">{{ t('batch.prev') }}</a-button>
        <span class="flex gap-2">
          <a-button @click="resetTasks">{{ t('common.reset') }}</a-button>
          <a-button type="primary" :loading="publishing" @click="handlePublish">
            {{ t('batch.publishN', { n: tasks.length }) }}
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
            <a-button type="primary" @click="restart">{{ t('batch.continue') }}</a-button>
          </div>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup lang="tsx">
import type { RouteMeta } from 'vue-router'
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
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

const step = ref(0)
const publishing = ref(false)

// ============ 元数据 ============
const meetings = ref<BatchMeeting[]>([])
const executors = ref<BatchExecutor[]>([])
const meetingOptions = computed(() => meetings.value.map((m) => ({ label: `${m.name}（${m.meetingDate}）`, value: m.id })))
const executorOptions = computed(() => executors.value.map((e) => ({ label: `${e.name} · ${e.deptName}`, value: e.id })))

const priorityOptions = computed(() => [
  { label: t('dict.priority.normal'), value: 'normal' },
  { label: t('dict.priority.important'), value: 'important' }
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
const bulk = reactive<{ priority?: string | undefined; deadline?: string | undefined; executorIds: string[] }>({
  priority: undefined,
  deadline: undefined,
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
const previewData = computed(() =>
  tasks.map((task, i) => ({
    no: i + 1,
    desc: task.desc,
    priority: task.priority,
    deadline: task.deadline,
    executorNames: task.executorIds.map(executorName).join('、')
  }))
)
const previewColumns: TableColumn[] = [
  { field: 'no', title: t('batch.col.no'), width: 60 },
  { field: 'desc', title: t('batch.col.desc'), minWidth: 240, slots: { default: ({ row }: any) => <span>{row.desc || '--'}</span> } },
  {
    field: 'priority',
    title: t('batch.col.priority'),
    width: 100,
    slots: { default: ({ row }: any) => <a-tag color={row.priority === 'important' ? 'red' : 'default'}>{t('dict.priority.' + row.priority)}</a-tag> }
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
  step.value = 1
}

const resultIds = ref<string[]>([])
async function handlePublish() {
  const valid = tasks.filter((task) => task.desc.trim())
  if (!valid.length) {
    message.warning(t('batch.taskRequired'))
    return
  }
  publishing.value = true
  try {
    await publishBatchTasks({
      meetingSource: meetingSource.value,
      meetingId: form.meetingId,
      meetingName: form.meetingName,
      meetingDate: form.meetingDate,
      recorderName: form.recorderName,
      tasks: valid.map((task) => ({
        desc: task.desc,
        priority: task.priority,
        deadline: task.deadline,
        executorIds: task.executorIds
      }))
    })
    resultIds.value = valid.map((_, i) => `TASK-${Date.now()}-${i + 1}`)
    step.value = 2
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
</style>
