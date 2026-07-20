import AIRequestGuard from '@ai-request-guard/core'
import type {
  CommentDTO,
  CommentNode,
  DiscussionDTO,
  DiscussionItem,
  DiscussionPageResult
} from './types'
import mockData from './mocks/discussion.json'

// 评论树递归映射
const mapComments = (list: CommentDTO[] | undefined): CommentNode[] =>
  (list ?? []).map((c) => ({
    id: String(c.comment_id),
    authorName: c.author_name ?? '',
    content: c.content ?? '',
    createdAt: c.created_at ?? '',
    children: mapComments(c.children)
  }))

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t() 渲染）
const toItem = (dto: DiscussionDTO): DiscussionItem => ({
  id: String(dto.post_id),
  title: dto.title ?? '',
  content: dto.content ?? '',
  topic: dto.topic ?? '',
  authorName: dto.author_name ?? '',
  replyCount: dto.reply_count ?? 0,
  viewCount: dto.view_count ?? 0,
  isHot: dto.is_hot ?? false,
  createdAt: dto.created_at ?? '',
  tags: dto.tags ?? [],
  comments: mapComments(dto.comments)
})

// ============ 分页列表 adapter ============
export const getDiscussionListAdapter = (raw: unknown): DiscussionPageResult => {
  const data = raw as { records: DiscussionDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    // 后端全局 Long→String 会把分页 total 也序列化成字符串（实测 "total":"1"），强制收敛为 number
    total: Number(data.total ?? 0)
  }
}

// ============ 详情 adapter ============
export const getDiscussionDetailAdapter = (raw: unknown): DiscussionItem => toItem(raw as DiscussionDTO)

// ============ 回帖单节点 adapter（discussion/reply 返回新建评论节点）============
export const toCommentNode = (raw: unknown): CommentNode => {
  const c = raw as CommentDTO
  return {
    id: String(c.comment_id),
    authorName: c.author_name ?? '',
    content: c.content ?? '',
    createdAt: c.created_at ?? '',
    children: mapComments(c.children)
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getDiscussionListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getDiscussionListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getDiscussionDetailAdapter(mockData.records[0]),
  adapter: getDiscussionDetailAdapter
})
