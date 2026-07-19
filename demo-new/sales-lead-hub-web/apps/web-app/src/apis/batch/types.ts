// ============ DTO（后端原始类型，snake_case）============
// 批量发布任务向导的元数据（会议 / 执行人可选项）。
export interface BatchMeetingDTO {
  meeting_id: string
  name: string
  meeting_date: string
  recorder_name: string
}
export interface BatchExecutorDTO {
  user_id: string
  name: string
  dept_name: string
}
export interface BatchMetaDTO {
  meetings: BatchMeetingDTO[]
  executors: BatchExecutorDTO[]
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface BatchMeeting {
  id: string
  name: string
  meetingDate: string
  recorderName: string
}
export interface BatchExecutor {
  id: string
  name: string
  deptName: string
}
export interface BatchMeta {
  meetings: BatchMeeting[]
  executors: BatchExecutor[]
}

// ============ 发布参数 ============
export type BatchTaskParams = {
  desc: string
  priority: string
  deadline: string
  executorIds: string[]
}
export type BatchPublishParams = {
  meetingSource: string // exist | new
  meetingId?: string | undefined
  meetingName?: string | undefined
  meetingDate?: string | undefined
  recorderName?: string | undefined
  tasks: BatchTaskParams[]
}
