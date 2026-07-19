<template>
  <div class="h-full p-[16px] bg-white rounded overflow-auto flex flex-col">
    <div class="flex items-center justify-between mb-3 flex-wrap gap-2">
      <h2 class="text-[18px] font-bold">{{ t('task.title') }}</h2>
      <div class="flex items-center gap-2">
        <a-select
          :value="priorityFilter"
          :options="priorityFilterOptions"
          allow-clear
          :placeholder="t('task.priorityFilter')"
          style="width: 130px"
          @update:value="(v: any) => (priorityFilter = v ?? undefined)"
        />
        <a-select :value="sortBy" :options="sortOptions" style="width: 140px" @update:value="(v: any) => (sortBy = v)" />
      </div>
    </div>

    <a-segmented v-model:value="activeTab" :options="tabOptions" class="mb-4 self-start" />

    <a-empty v-if="!filteredList.length" :description="t('common.noData')" class="mt-10" />

    <a-list v-else :data-source="filteredList" :grid="{ gutter: 16, column: 2, xs: 1, sm: 1, md: 2 }">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-card size="small" :bordered="true" :class="{ 'task-transferred': item.transferFrom }">
            <!-- 转交来源徽标 -->
            <div v-if="item.transferFrom" class="transfer-badge">
              <SwapOutlined />
              {{ t('task.transferFromLabel') }} <strong>{{ item.transferFrom }}</strong>
            </div>

            <div class="flex items-center gap-2 mb-2 flex-wrap">
              <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ item.id }}</span>
              <span class="font-semibold text-[hsl(var(--text))]">{{ item.meetingName }}</span>
              <a-tag :color="priorityColor[item.priority] || 'default'">{{ t('dict.urgency.' + item.priority) }}</a-tag>
              <a-tag :color="statusColor[item.status] || 'default'">{{ t('dict.taskStatus.' + item.status) }}</a-tag>
            </div>

            <!-- 元信息：会议日期 / 记录人 / 截止 / 协作人 -->
            <div class="task-meta">
              <span><CalendarOutlined /> {{ item.meetingDate || '--' }}</span>
              <span><UserOutlined /> {{ item.recorderName || '--' }}</span>
              <span :class="isOverdue(item) ? 'text-[hsl(var(--error))] font-medium' : ''">
                <ClockCircleOutlined /> {{ t('task.deadline') }}: {{ item.deadline || '--' }}
                <template v-if="isOverdue(item)"> ({{ t('meeting.overdue') }})</template>
              </span>
              <span v-if="item.assigneeNames && item.assigneeNames.length > 1">
                <TeamOutlined /> {{ t('task.collaborators') }}: {{ item.assigneeNames.join('、') }}
              </span>
            </div>

            <!-- 描述（可展开） -->
            <a-typography-paragraph
              class="task-desc"
              :content="item.taskDesc"
              :ellipsis="{ rows: 2, expandable: true, symbol: t('common.more') }"
            />

            <!-- 转交记录 -->
            <div v-if="item.transferHistory && item.transferHistory.length" class="transfer-history">
              <div class="th-title"><HistoryOutlined /> {{ t('task.transferHistory') }}</div>
              <div v-for="(h, i) in item.transferHistory" :key="i" class="th-row">
                {{ h.time }} · {{ h.from }} → {{ h.to }}：{{ h.reason }}
              </div>
            </div>

            <div class="flex items-center justify-between pt-2 border-t border-[hsl(var(--line))]">
              <span class="text-[12px] text-[hsl(var(--secondary-text))]">{{ t('task.createdAtLabel') }} {{ item.createdAt }}</span>
              <div class="flex gap-2">
                <template v-if="item.status === 'pending'">
                  <a-button type="primary" size="small" @click="handleStart(item)">{{ t('task.start') }}</a-button>
                  <a-button size="small" @click="openTransfer(item)">{{ t('task.transfer') }}</a-button>
                </template>
                <template v-else-if="item.status === 'processing'">
                  <a-button type="primary" size="small" @click="openComplete(item)">{{ t('task.complete') }}</a-button>
                  <a-button size="small" @click="openTransfer(item)">{{ t('task.transfer') }}</a-button>
                </template>
                <a-tag v-else-if="item.status === 'completed'" color="green">{{ t('dict.taskStatus.completed') }}</a-tag>
                <span v-else-if="item.status === 'transferred'" class="text-[13px] text-[hsl(var(--secondary-text))]">
                  {{ t('task.waiting') }}
                </span>
              </div>
            </div>
          </a-card>
        </a-list-item>
      </template>
    </a-list>

    <!-- 转交 Modal -->
    <a-modal v-model:open="transferOpen" :title="t('task.transfer')" :ok-text="t('task.transfer')" @ok="confirmTransfer">
      <div class="mb-3">
        <div class="mb-1">{{ t('task.transferTo') }}</div>
        <a-select
          :value="transferTo"
          :options="candidateOptions"
          show-search
          :placeholder="t('common.selectPlaceholder')"
          style="width: 100%"
          @update:value="(v: any) => (transferTo = v)"
        />
      </div>
      <div>
        <div class="mb-1">{{ t('task.transferReason') }}</div>
        <a-textarea v-model:value="transferReason" :rows="3" :placeholder="t('task.transferReasonPlaceholder')" :maxlength="200" show-count />
      </div>
    </a-modal>

    <!-- 完成 Modal -->
    <a-modal v-model:open="completeOpen" :title="t('task.complete')" :ok-text="t('task.complete')" @ok="confirmComplete">
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
import {
  SwapOutlined, CalendarOutlined, UserOutlined, ClockCircleOutlined, TeamOutlined, HistoryOutlined
} from '@ant-design/icons-vue'
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
  completed: 'green',
  transferred: 'purple',
  cancelled: 'default'
}
const priorityColor: Record<string, string> = {
  normal: 'default',
  urgent: 'orange',
  critical: 'red'
}

// Mock 用户列表（转交对象来源）
const SALES_USERS = ['李娜', '王强', '赵敏', '陈涛', '周杰', '刘洋', '郑浩', '吴敏', '冯雪', '孙丽']

const list = ref<TaskItem[]>([])
const activeTab = ref<string>('all')
const priorityFilter = ref<string | undefined>(undefined)
const sortBy = ref<'deadline' | 'priority'>('deadline')

const priorityFilterOptions = computed(() => [
  { label: t('dict.urgency.critical'), value: 'critical' },
  { label: t('dict.urgency.urgent'), value: 'urgent' },
  { label: t('dict.urgency.normal'), value: 'normal' }
])
const sortOptions = computed(() => [
  { label: t('task.sortByDeadline'), value: 'deadline' },
  { label: t('task.sortByPriority'), value: 'priority' }
])

const candidateOptions = computed(() => SALES_USERS.map((n) => ({ label: n, value: n })))

function countOf(status: string) {
  return list.value.filter((i) => i.status === status).length
}

const tabOptions = computed(() => [
  { label: `${t('common.all')} (${list.value.length})`, value: 'all' },
  { label: `${t('dict.taskStatus.pending')} (${countOf('pending')})`, value: 'pending' },
  { label: `${t('dict.taskStatus.processing')} (${countOf('processing')})`, value: 'processing' },
  { label: `${t('dict.taskStatus.completed')} (${countOf('completed')})`, value: 'completed' },
  { label: `${t('dict.taskStatus.transferred')} (${countOf('transferred')})`, value: 'transferred' }
])

const PRI_ORDER: Record<string, number> = { critical: 0, urgent: 1, normal: 2 }
const filteredList = computed(() => {
  const out = list.value.filter((i) => {
    const byTab = activeTab.value === 'all' || i.status === activeTab.value
    const byPriority = !priorityFilter.value || i.priority === priorityFilter.value
    return byTab && byPriority
  })
  return out.slice().sort((a, b) => {
    if (sortBy.value === 'priority') return (PRI_ORDER[a.priority] ?? 2) - (PRI_ORDER[b.priority] ?? 2)
    return new Date((a.deadline || '').replace(/-/g, '/')).getTime() - new Date((b.deadline || '').replace(/-/g, '/')).getTime()
  })
})

function isOverdue(item: TaskItem) {
  if (item.status === 'completed' || item.status === 'cancelled' || item.status === 'transferred') return false
  return new Date((item.deadline || '').replace(/-/g, '/')).getTime() < Date.now()
}

async function loadList() {
  const result = await getTaskList({ pageNumber: 1, pageSize: 100 })
  list.value = result.records
}

async function handleStart(item: TaskItem) {
  item.status = 'processing'
  list.value = [...list.value]
  startTask(item.id).catch(() => {})
  message.success(t('common.success'))
}

// ============ 转交 Modal ============
const transferOpen = ref(false)
const transferTo = ref<string | undefined>(undefined)
const transferReason = ref('')
const transferTarget = ref<TaskItem | null>(null)
function openTransfer(item: TaskItem) {
  transferTarget.value = item
  transferTo.value = undefined
  transferReason.value = ''
  transferOpen.value = true
}
function confirmTransfer() {
  const to = transferTo.value
  const reason = transferReason.value
  if (!to) {
    message.warning(t('task.transferToPlaceholder'))
    return
  }
  if (!reason) {
    message.warning(t('task.transferReasonPlaceholder'))
    return
  }
  const target = transferTarget.value
  if (target) {
    target.status = 'transferred'
    target.transferHistory = [
      ...(target.transferHistory || []),
      {
        time: new Date().toISOString().slice(0, 16).replace('T', ' '),
        from: target.assigneeNames?.[0] || t('task.me'),
        to,
        reason
      }
    ]
    list.value = [...list.value]
    transferTask({ id: target.id, transferTo: to, reason }).catch(() => {})
  }
  message.success(t('task.transferSuccess'))
  transferOpen.value = false
}

// ============ 完成 Modal ============
const completeOpen = ref(false)
const completeRemark = ref('')
const completeTarget = ref<TaskItem | null>(null)
function openComplete(item: TaskItem) {
  completeTarget.value = item
  completeRemark.value = ''
  completeOpen.value = true
}
function confirmComplete() {
  const target = completeTarget.value
  if (target) {
    target.status = 'completed'
    list.value = [...list.value]
    completeTask(target.id, completeRemark.value).catch(() => {})
  }
  message.success(t('task.completeSuccess'))
  completeOpen.value = false
}

onMounted(loadList)
</script>

<style scoped>
.task-transferred {
  border-left: 3px solid hsl(var(--primary));
}
.transfer-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  margin-bottom: 8px;
  background: hsl(var(--primary) / 0.08);
  border: 1px solid hsl(var(--primary) / 0.2);
  border-radius: 6px;
  font-size: 12px;
  color: hsl(var(--primary));
}
.task-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 12px;
  color: hsl(var(--secondary-text));
  margin-bottom: 8px;
}
.task-desc {
  color: hsl(var(--text));
  margin-bottom: 10px !important;
}
.transfer-history {
  background: hsl(var(--card-bg));
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 12px;
}
.transfer-history .th-title {
  color: hsl(var(--secondary-text));
  margin-bottom: 4px;
}
.transfer-history .th-row {
  color: hsl(var(--secondary-text));
  margin-top: 2px;
}
</style>
