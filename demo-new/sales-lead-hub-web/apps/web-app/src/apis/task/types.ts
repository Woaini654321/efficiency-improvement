import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ 转交记录（快照）============
export interface TaskTransferRecord {
  time: string
  from: string
  to: string
  reason: string
}

// ============ DTO（后端原始类型，snake_case）============
export interface TaskDTO {
  task_id: string
  meeting_name: string
  meeting_date: string
  priority: string // normal | urgent | critical
  status: string // pending | processing | completed | transferred | cancelled
  deadline: string
  task_desc: string
  recorder_name: string
  transfer_from: string
  assignee_names: string[]
  transfer_history: TaskTransferRecord[]
  created_at: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface TaskItem {
  id: string
  meetingName: string
  meetingDate: string
  priority: string
  status: string
  deadline: string
  taskDesc: string
  recorderName: string
  transferFrom: string
  assigneeNames: string[]
  transferHistory: TaskTransferRecord[]
  createdAt: string
}

// ============ 分页 ============
export type TaskPageParams = PaginationParams<{
  status?: string
  priority?: string
  sort?: string
}>
export type TaskPageResult = PaginationResult<TaskItem>

// ============ 操作参数（type 别名，满足 request BodyArg 索引签名）============
export type TaskTransferParams = {
  id: string
  transferTo: string
  reason: string
}
