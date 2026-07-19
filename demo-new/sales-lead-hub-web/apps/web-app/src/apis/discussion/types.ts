import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ 评论树 ============
export interface CommentDTO {
  comment_id: string
  author_name: string
  content: string
  created_at: string
  children?: CommentDTO[]
}
export interface CommentItem {
  id: string
  authorName: string
  content: string
  createdAt: string
  children: CommentItem[]
}

// ============ DTO（后端原始类型，snake_case）============
export interface DiscussionDTO {
  post_id: string
  title: string
  content: string
  topic: string // business | solution | experience | industry | complaint
  author_name: string
  reply_count: number
  view_count: number
  is_hot: boolean
  created_at: string
  comments?: CommentDTO[]
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface DiscussionItem {
  id: string
  title: string
  content: string
  topic: string
  authorName: string
  replyCount: number
  viewCount: number
  isHot: boolean
  createdAt: string
  comments: CommentItem[]
}

// ============ 分页 ============
export type DiscussionPageParams = PaginationParams<{
  keyword?: string
  topic?: string
}>
export type DiscussionPageResult = PaginationResult<DiscussionItem>

// ============ 增改参数（type 别名，满足 request BodyArg 索引签名）============
export type DiscussionCreateParams = {
  title: string
  topic: string
  content: string
}
