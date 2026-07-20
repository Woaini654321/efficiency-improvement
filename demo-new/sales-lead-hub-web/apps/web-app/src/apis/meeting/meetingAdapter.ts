import AIRequestGuard from '@ai-request-guard/core'
import type { MeetingTaskDTO, MeetingTaskItem, MeetingTaskPageResult } from './types'
import mockData from './mocks/meeting.json'

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染）
const toItem = (dto: MeetingTaskDTO): MeetingTaskItem => ({
  id: String(dto.task_id),
  meetingName: dto.meeting_name ?? '',
  meetingDate: dto.meeting_date ?? '',
  recorderName: dto.recorder_name ?? '',
  taskDesc: dto.task_desc ?? '',
  priority: dto.priority ?? '',
  deadline: dto.deadline ?? '',
  assigneeNames: dto.assignee_names ?? [],
  status: dto.status ?? '',
  createdAt: dto.created_at ?? ''
})

// ============ 分页列表 adapter ============
export const getMeetingListAdapter = (raw: unknown): MeetingTaskPageResult => {
  const data = raw as { records: MeetingTaskDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    // 后端全局 Long→String 会把分页 total 也序列化成字符串（实测 "total":"1"），强制收敛为 number
    total: Number(data.total ?? 0)
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getMeetingListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getMeetingListAdapter
})
