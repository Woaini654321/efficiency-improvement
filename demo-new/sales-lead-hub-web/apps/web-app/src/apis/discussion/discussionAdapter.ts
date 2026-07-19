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
    total: data.total ?? 0
  }
}

// ============ 详情 adapter ============
export const getDiscussionDetailAdapter = (raw: unknown): DiscussionItem => toItem(raw as DiscussionDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getDiscussionListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getDiscussionListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getDiscussionDetailAdapter(mockData.records[0]),
  adapter: getDiscussionDetailAdapter
})
