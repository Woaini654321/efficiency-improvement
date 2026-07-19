import AIRequestGuard from '@ai-request-guard/core'
import type { BatchMetaDTO, BatchMeta } from './types'
import mockData from './mocks/batch.json'

// ============ 元数据 adapter（raw 已解包）============
export const getBatchMetaAdapter = (raw: unknown): BatchMeta => {
  const dto = (raw ?? {}) as BatchMetaDTO
  return {
    meetings: (dto.meetings ?? []).map((m) => ({
      id: String(m.meeting_id),
      name: m.name ?? '',
      meetingDate: m.meeting_date ?? '',
      recorderName: m.recorder_name ?? ''
    })),
    executors: (dto.executors ?? []).map((e) => ({
      id: String(e.user_id),
      name: e.name ?? '',
      deptName: e.dept_name ?? ''
    }))
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getBatchMetaAdapter(mockData),
  adapter: getBatchMetaAdapter
})
