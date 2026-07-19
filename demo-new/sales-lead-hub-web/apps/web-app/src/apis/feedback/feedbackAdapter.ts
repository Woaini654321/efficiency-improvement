import AIRequestGuard from '@ai-request-guard/core'
import type { FeedbackDTO, FeedbackItem, FeedbackListResult } from './types'
import mockData from './mocks/feedback.json'

// 单条 DTO → ViewModel
const toItem = (dto: FeedbackDTO): FeedbackItem => ({
  id: String(dto.feedback_id),
  title: dto.title ?? '',
  content: dto.content ?? '',
  anonName: dto.anon_name ?? '',
  likeCount: dto.like_count ?? 0,
  createdAt: dto.created_at ?? ''
})

// ============ 列表 adapter ============
export const getFeedbackListAdapter = (raw: unknown): FeedbackListResult => {
  const data = raw as { records: FeedbackDTO[] }
  return (data.records ?? []).map(toItem)
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getFeedbackListAdapter({ records: mockData.records }),
  adapter: getFeedbackListAdapter
})
