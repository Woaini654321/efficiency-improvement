import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
export interface TaskDTO {
  task_id: string
  meeting_name: string
  priority: string // normal | urgent | critical
  status: string // pending | processing | done | transferred | cancelled
  deadline: string
  task_desc: string
  recorder_name: string
  transfer_from: string
  created_at: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface TaskItem {
  id: string
  meetingName: string
  priority: string
  status: string
  deadline: string
  taskDesc: string
  recorderName: string
  transferFrom: string
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
