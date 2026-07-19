import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
// content_audit 单表：publisher_name 为冗余快照字段，满足「一个展示字段只来源一张表」。
export interface ContentAuditDTO {
  audit_id: string
  title: string
  content_type: string // opportunity | request
  publisher_name: string
  status: string // published | archived | pending | collecting | adopted | closed
  is_pinned: boolean
  published_at?: string
  sort_no: number
  urgency: string // normal | urgent | critical
  industry?: string
  tags?: string[]
  category_path?: string[][]
  description?: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface AuditItem {
  id: string
  title: string
  contentType: string
  publisherName: string
  status: string
  isPinned: boolean
  publishedAt: string
  sortNo: number
  urgency: string
  industry: string
  tags: string[]
  categoryPath: string[][]
  description: string
}

// ============ 分页 ============
export type AuditPageParams = PaginationParams<{
  keyword?: string
  contentType?: string
  status?: string
}>
export type AuditPageResult = PaginationResult<AuditItem>

// ============ 增删改参数（用 type 别名，满足 request BodyArg 索引签名）============
export type AuditStatusParams = {
  id: string
  status: string
}
export type AuditPinParams = {
  id: string
  isPinned: boolean
}
