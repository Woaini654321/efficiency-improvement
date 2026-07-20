import AIRequestGuard from '@ai-request-guard/core'
import type { TaskDTO, TaskItem, TaskPageResult } from './types'
import mockData from './mocks/task.json'

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染）
const toItem = (dto: TaskDTO): TaskItem => ({
  id: String(dto.task_id),
  meetingName: dto.meeting_name ?? '',
  meetingDate: dto.meeting_date ?? '',
  priority: dto.priority ?? '',
  status: dto.status ?? '',
  deadline: dto.deadline ?? '',
  taskDesc: dto.task_desc ?? '',
  recorderName: dto.recorder_name ?? '',
  transferFrom: dto.transfer_from ?? '',
  assigneeNames: dto.assignee_names ?? [],
  transferHistory: dto.transfer_history ?? [],
  createdAt: dto.created_at ?? ''
})

// ============ 分页列表 adapter ============
export const getTaskListAdapter = (raw: unknown): TaskPageResult => {
  const data = raw as { records: TaskDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    // 后端全局 Long→String 会把分页 total 也序列化成字符串（实测 "total":"1"），强制收敛为 number
    total: Number(data.total ?? 0)
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getTaskListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getTaskListAdapter
})
