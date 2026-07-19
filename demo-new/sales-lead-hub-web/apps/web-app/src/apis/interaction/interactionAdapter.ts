import AIRequestGuard from '@ai-request-guard/core'
import type { CommentDTO, Comment } from './types'
import mockData from './mocks/interaction.json'

// 单条回复（二级，replies 恒为空）
const toReply = (dto: CommentDTO): Comment => ({
  id: String(dto.interaction_id),
  authorName: dto.author_name ?? '',
  authorDept: dto.author_dept ?? '',
  content: dto.content ?? '',
  likeCount: dto.like_count ?? 0,
  createdAt: dto.created_at ?? '',
  replies: []
})

// 单条顶级评论（含一层回复）
const toComment = (dto: CommentDTO): Comment => ({
  id: String(dto.interaction_id),
  authorName: dto.author_name ?? '',
  authorDept: dto.author_dept ?? '',
  content: dto.content ?? '',
  likeCount: dto.like_count ?? 0,
  createdAt: dto.created_at ?? '',
  replies: (dto.replies ?? []).map(toReply)
})

// ============ 评论列表 adapter（raw 为 DTO 数组，已解包）============
export const getCommentsAdapter = (raw: unknown): Comment[] => {
  const list = (raw as CommentDTO[] | null) ?? []
  return list.map(toComment)
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getCommentsAdapter(mockData.comments),
  adapter: getCommentsAdapter
})
