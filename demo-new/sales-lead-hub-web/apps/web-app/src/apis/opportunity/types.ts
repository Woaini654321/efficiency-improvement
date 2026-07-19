import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ 附件 ============
export interface AttachmentDTO {
  name: string
  url: string
  size: number
}
export interface AttachmentItem {
  uid: string
  name: string
  url: string
  size: number
}

// ============ DTO（后端原始类型，snake_case）============
// 说明：publisher_name / publisher_dept_name / category_names 为冗余快照字段，
// 落在 opportunities 单表，满足「一个展示字段只来源一张表」。
export interface OpportunityDTO {
  opportunity_id: string
  title: string
  summary?: string
  content: string
  type: string // product_info | solution | success_case
  status: string // draft | published | archived
  publisher_name: string
  publisher_dept_name: string
  category_names: string[]
  cover_url?: string
  is_pinned: boolean
  view_count: number
  like_count: number
  collect_count: number
  comment_count: number
  attachments: AttachmentDTO[]
  created_at: string
  published_at?: string
  // 过期/临期治理冗余快照列（落 opportunities 单表）
  expiry_date?: string | null
  superseded_by?: string | null
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface OpportunityItem {
  id: string
  title: string
  summary: string
  content: string
  type: string
  status: string
  publisherName: string
  publisherDeptName: string
  categoryNames: string[]
  coverUrl: string
  isPinned: boolean
  viewCount: number
  likeCount: number
  collectCount: number
  commentCount: number
  attachments: AttachmentItem[]
  createdAt: string
  publishedAt: string
  // 过期日期与「已被替代」的新版本 id（空则未过期/无新版本）
  expiryDate: string
  supersededBy: string
}

// ============ 分页 ============
export type OpportunityPageParams = PaginationParams<{
  keyword?: string
  type?: string
  status?: string
  sort?: string
}>
export type OpportunityPageResult = PaginationResult<OpportunityItem>

// ============ 增删改参数（用 type 别名，满足 request BodyArg 的 Record<string,JsonValue> 索引签名）============
export type OpportunityCreateParams = {
  publisherId?: string
  title: string
  type: string
  categoryIds: string[]
  industry?: string
  keywords?: string[]
  summary?: string
  content: string
}
export type OpportunityUpdateParams = OpportunityCreateParams & {
  id: string
}
