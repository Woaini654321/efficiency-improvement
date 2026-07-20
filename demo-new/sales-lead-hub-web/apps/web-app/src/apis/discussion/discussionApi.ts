import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getDiscussionListAdapter, getDiscussionDetailAdapter, toCommentNode } from './discussionAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/discussion.json'
import type {
  DiscussionPageParams,
  DiscussionPageResult,
  DiscussionItem,
  DiscussionCreateParams,
  DiscussionReplyParams,
  CommentNode
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询讨论帖列表 */
export const getDiscussionList = async (
  params: DiscussionPageParams
): Promise<DiscussionPageResult> => {
  return (await AIRequestGuard({
    adapter: getDiscussionListAdapter,
    request: mockRequest('discussion',
      { records: mockData.records, total: mockData.total },
      () => request.POST<DiscussionPageResult>({ url: 'discussion/page' }, params)
    )
  })) as DiscussionPageResult
}

/** 查询讨论帖详情 */
export const getDiscussionDetail = async (id: string): Promise<DiscussionItem> => {
  return (await AIRequestGuard({
    adapter: getDiscussionDetailAdapter,
    request: mockRequest('discussion',
      mockData.records[0],
      () => request.GET<DiscussionItem>({ url: 'discussion/detail' }, { id })
    )
  })) as DiscussionItem
}

// ============ 增（直接 request）============

/** 发布讨论帖 */
export const createDiscussion = async (params: DiscussionCreateParams): Promise<void> => {
  await request.POST({ url: 'discussion/create' }, params)
}

/** 回帖：返回新建评论节点（{ comment_id, author_name, content, created_at, children:[] }），供页面插入评论树 */
export const replyDiscussion = async (params: DiscussionReplyParams): Promise<CommentNode> => {
  const raw = await request.POST<CommentNode>({ url: 'discussion/reply' }, params)
  return toCommentNode(raw)
}
