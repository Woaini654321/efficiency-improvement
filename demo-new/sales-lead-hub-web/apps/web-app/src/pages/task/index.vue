<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto flex flex-col">
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-[18px] font-bold">{{ t('task.title') }}</h2>
      <a-select
        v-model:value="priorityFilter"
        :options="priorityFilterOptions"
        allow-clear
        :placeholder="t('task.priorityFilter')"
        style="width: 160px"
      />
    </div>

    <a-segmented v-model:value="activeTab" :options="tabOptions" class="mb-4 self-start" />

    <a-empty v-if="!filteredList.length" :description="t('common.noData')" class="mt-10" />

    <a-list v-else :data-source="filteredList" :grid="{ gutter: 16, column: 2, xs: 1, sm: 1, md: 2 }">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-card size="small" :bordered="true">
            <div class="flex items-center gap-2 mb-2 flex-wrap">
              <a-tag v-if="item.transferFrom" color="default">{{ t('task.transferFromLabel') }}: {{ item.transferFrom }}</a-tag>
              <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ item.id }}</span>
              <span class="font-semibold text-[hsl(var(--text))]">{{ item.meetingName }}</span>
              <a-tag :color="priorityColor[item.priority] ?? 'default'">{{ t('dict.urgency.' + item.priority) }}</a-tag>
              <a-tag :color="statusColor[item.status] ?? 'default'">{{ t('dict.taskStatus.' + item.status) }}</a-tag>
            </div>

            <div class="text-[hsl(var(--text))] leading-6 mb-2">{{ item.taskDesc }}</div>

            <div class="text-[13px] text-[hsl(var(--secondary-text))] mb-1">
              {{ t('task.recorder') }}: {{ item.recorderName || '--' }}
            </div>
            <div class="text-[13px] mb-2">
              <span class="text-[hsl(var(--secondary-text))]">{{ t('task.deadline') }}: </span>
              <span :class="isOverdue(item) ? 'text-[hsl(var(--error))] font-medium' : 'text-[hsl(var(--text))]'">
                {{ item.deadline || '--' }}
              </span>
            </div>

            <div class="flex justify-end gap-2 pt-2 border-t border-[hsl(var(--line))]">
              <template v-if="item.status === 'pending'">
                <a-button type="primary" size="small" @click="handleStart(item)">{{ t('task.start') }}</a-button>
                <a-button size="small" @click="openTransfer(item)">{{ t('task.transfer') }}</a-button>
              </template>
              <template v-else-if="item.status === 'processing'">
                <a-button type="primary" size="small" @click="openComplete(item)">{{ t('task.complete') }}</a-button>
                <a-button size="small" @click="openTransfer(item)">{{ t('task.transfer') }}</a-button>
              </template>
              <a-tag v-else-if="item.status === 'done'" color="green">{{ t('dict.taskStatus.done') }}</a-tag>
              <span v-else-if="item.status === 'transferred'" class="text-[13px] text-[hsl(var(--secondary-text))]">
                {{ t('task.waiting') }}
              </span>
            </div>
          </a-card>
        </a-list-item>
      </template>
    </a-list>

    <!-- 转交 Modal -->
    <a-modal
      v-model:open="transferOpen"
      :title="t('task.transfer')"
      :ok-text="t('task.transfer')"
      @ok="confirmTransfer"
    >
      <div class="mb-3">
        <div class="mb-1">{{ t('task.transferTo') }}</div>
        <a-select
          v-model:value="transferTo"
          :options="candidateOptions"
          show-search
          :placeholder="t('common.selectPlaceholder')"
          style="width: 100%"
        />
      </div>
      <div>
        <div class="mb-1">{{ t('task.transferReason') }}</div>
        <a-textarea v-model:value="transferReason" :rows="3" :placeholder="t('task.transferReasonPlaceholder')" :maxlength="200" show-count />
      </div>
    </a-modal>

    <!-- 完成 Modal -->
    <a-modal
      v-model:open="completeOpen"
      :title="t('task.complete')"
      :ok-text="t('task.complete')"
      @ok="confirmComplete"
    >
      <div class="mb-1">{{ t('task.completeRemark') }}</div>
      <a-textarea v-model:value="completeRemark" :rows="3" :placeholder="t('task.completeRemarkPlaceholder')" :maxlength="200" show-count />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import type { RouteMeta } from 'vue-router'
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getTaskList, startTask, completeTask, transferTask } from '@/apis/task/taskApi'
import type { TaskItem } from '@/apis/task/types'

defineOptions({ name: 'MyTask' })
definePage({
  name: 'MyTask',
  meta: {
    layout: 'default',
    menu: { icon: 'q-icon:point-list-linear' },
    title: 'task'
    // authCode: '',
    // keepAlive: true,
  } satisfies RouteMeta
})

const { t } = useI18n()

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

const list = ref<TaskItem[]>([])
const activeTab = ref<string>('all')
const priorityFilter = ref<string | undefined>(undefined)

const priorityFilterOptions = computed(() => [
  { label: t('dict.urgency.normal'), value: 'normal' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.critical'), value: 'critical' }
])

// 转交候选人（后端接入后由用户列表接口提供，此处为 mock 候选）
const candidateOptions = computed(() =>
  ['李娜', '王强', '赵敏', '陈涛', '周杰', '刘洋', '郑浩'].map((n) => ({ label: n, value: n }))
)

function countOf(status: string) {
  return list.value.filter((i) => i.status === status).length
}

const tabOptions = computed(() => [
  { label: `${t('common.all')} (${list.value.length})`, value: 'all' },
  { label: `${t('dict.taskStatus.pending')} (${countOf('pending')})`, value: 'pending' },
  { label: `${t('dict.taskStatus.processing')} (${countOf('processing')})`, value: 'processing' },
  { label: `${t('dict.taskStatus.done')} (${countOf('done')})`, value: 'done' },
  { label: `${t('dict.taskStatus.transferred')} (${countOf('transferred')})`, value: 'transferred' }
])

const filteredList = computed(() =>
  list.value.filter((i) => {
    const byTab = activeTab.value === 'all' || i.status === activeTab.value
    const byPriority = !priorityFilter.value || i.priority === priorityFilter.value
    return byTab && byPriority
  })
)

function isOverdue(item: TaskItem) {
  if (item.status === 'done' || item.status === 'cancelled') return false
  return new Date(item.deadline).getTime() < Date.now()
}

async function loadList() {
  const result = await getTaskList({ pageNumber: 1, pageSize: 100 })
  list.value = result.records
}

async function handleStart(item: TaskItem) {
  await startTask(item.id)
  message.success(t('common.success'))
  await loadList()
}

// ============ 转交 Modal ============
const transferOpen = ref(false)
const transferTo = ref<string | undefined>(undefined)
const transferReason = ref('')
const transferId = ref('')
function openTransfer(item: TaskItem) {
  transferId.value = item.id
  transferTo.value = undefined
  transferReason.value = ''
  transferOpen.value = true
}
async function confirmTransfer() {
  if (!transferTo.value) {
    message.warning(t('task.transferToPlaceholder'))
    return
  }
  if (!transferReason.value) {
    message.warning(t('task.transferReasonPlaceholder'))
    return
  }
  await transferTask({ id: transferId.value, transferTo: transferTo.value, reason: transferReason.value })
  message.success(t('task.transferSuccess'))
  transferOpen.value = false
  await loadList()
}

// ============ 完成 Modal ============
const completeOpen = ref(false)
const completeRemark = ref('')
const completeId = ref('')
function openComplete(item: TaskItem) {
  completeId.value = item.id
  completeRemark.value = ''
  completeOpen.value = true
}
async function confirmComplete() {
  await completeTask(completeId.value, completeRemark.value)
  message.success(t('task.completeSuccess'))
  completeOpen.value = false
  await loadList()
}

onMounted(loadList)
</script>
