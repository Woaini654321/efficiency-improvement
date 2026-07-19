import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
export interface MeetingTaskDTO {
  task_id: string
  meeting_name: string
  meeting_date: string
  recorder_name: string
  task_desc: string
  priority: string // normal | urgent | critical
  deadline: string
  assignee_names: string[]
  status: string // pending | processing | completed | transferred | cancelled
  created_at: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface MeetingTaskItem {
  id: string
  meetingName: string
  meetingDate: string
  recorderName: string
  taskDesc: string
  priority: string
  deadline: string
  assigneeNames: string[]
  status: string
  createdAt: string
}

// ============ 分页 ============
export type MeetingTaskPageParams = PaginationParams<{
  keyword?: string
  status?: string
  priority?: string
}>
export type MeetingTaskPageResult = PaginationResult<MeetingTaskItem>

// ============ 增改参数（type 别名，满足 request BodyArg 索引签名）============
export type MeetingTaskSaveParams = {
  id?: string | undefined
  meetingName: string
  meetingDate: string
  recorderName: string
  taskDesc: string
  priority: string
  deadline: string
  assigneeNames: string[]
}
