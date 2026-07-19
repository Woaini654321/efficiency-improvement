import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getDiscussionListAdapter, getDiscussionDetailAdapter } from './discussionAdapter'
import type {
  DiscussionPageParams,
  DiscussionPageResult,
  DiscussionItem,
  DiscussionCreateParams
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询讨论帖列表 */
export const getDiscussionList = async (
  params: DiscussionPageParams
): Promise<DiscussionPageResult> => {
  return (await AIRequestGuard({
    adapter: getDiscussionListAdapter,
    request: () => request.POST<DiscussionPageResult>({ url: 'discussion/page' }, params)
  })) as DiscussionPageResult
}

/** 查询讨论帖详情 */
export const getDiscussionDetail = async (id: string): Promise<DiscussionItem> => {
  return (await AIRequestGuard({
    adapter: getDiscussionDetailAdapter,
    request: () => request.GET<DiscussionItem>({ url: 'discussion/detail' }, { id })
  })) as DiscussionItem
}

// ============ 增（直接 request）============

/** 发布讨论帖 */
export const createDiscussion = async (params: DiscussionCreateParams): Promise<void> => {
  await request.POST({ url: 'discussion/create' }, params)
}
