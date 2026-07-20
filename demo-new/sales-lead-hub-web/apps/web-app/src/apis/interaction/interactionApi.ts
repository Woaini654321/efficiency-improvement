import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getCommentsAdapter } from './interactionAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/interaction.json'
import type { Comment, AddCommentParams, LikeTargetParams } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 查询目标对象的评论列表（≤2级） */
export const getComments = async (targetType: string, targetId: string): Promise<Comment[]> => {
  return (await AIRequestGuard({
    adapter: getCommentsAdapter,
    request: mockRequest('interaction',
      mockData.comments,
      () => request.POST<Comment[]>({ url: 'interaction/comments' }, { targetType, targetId })
    )
  })) as Comment[]
}

// ============ 增改（直接 request）============

/** 点赞评论 */
export const likeComment = async (id: string): Promise<void> => {
  await request.POST({ url: 'interaction/like' }, { id })
}

/** 发表评论 / 回复（parentId 为空即顶级评论） */
export const addComment = async (params: AddCommentParams): Promise<void> => {
  await request.POST({ url: 'interaction/comment' }, params)
}

/**
 * 对内容主体（商机/需求）点赞或收藏切换。
 * 后端 interaction/like 按 targetType+type 原子回写目标计数列，返回 ReactionVO（前端本地翻转，忽略 body）。
 */
export const likeTarget = async (params: LikeTargetParams): Promise<void> => {
  await request.POST({ url: 'interaction/like' }, params)
}
