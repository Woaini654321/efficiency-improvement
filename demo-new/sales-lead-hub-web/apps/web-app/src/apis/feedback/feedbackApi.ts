import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getFeedbackListAdapter } from './feedbackAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/feedback.json'
import type { FeedbackCreateParams, FeedbackListResult } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 查询吐槽列表 */
export const getFeedbackList = async (): Promise<FeedbackListResult> => {
  return (await AIRequestGuard({
    adapter: getFeedbackListAdapter,
    request: mockRequest(
      { records: mockData.records },
      () => request.GET<FeedbackListResult>({ url: 'feedback/list' })
    )
  })) as FeedbackListResult
}

// ============ 增改（直接 request）============

/** 发布一条匿名吐槽 */
export const createFeedback = async (params: FeedbackCreateParams): Promise<void> => {
  await request.POST({ url: 'feedback/create' }, params)
}

/** 点赞吐槽 */
export const likeFeedback = async (id: string): Promise<void> => {
  await request.POST({ url: 'feedback/like' }, { id })
}
