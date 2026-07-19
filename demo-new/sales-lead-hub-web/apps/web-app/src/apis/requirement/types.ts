import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ 方案响应 ============
// 说明：responder_name / responder_dept_name 为冗余快照字段，落在 solution_responses 单表。
export interface RequirementResponseDTO {
  response_id: string
  responder_name: string
  responder_dept_name: string
  content: string
  is_adopted: boolean
  created_at: string
  product_line_name?: string
  files?: string[]
}
export interface RequirementResponseItem {
  id: string
  responderName: string
  responderDeptName: string
  content: string
  isAdopted: boolean
  createdAt: string
  productLineName: string
  files: string[]
}

// ============ 邀请产品线（含响应进度）============
export interface InvitedProductLineDTO {
  name: string
  responded: boolean
  responder_count: number
}
export interface InvitedProductLineItem {
  name: string
  responded: boolean
  responderCount: number
}

// ============ DTO（后端原始类型，snake_case）============
// 说明：publisher_name / publisher_dept_name / category_names / invited_product_line_names
// 为冗余快照字段，落在 opportunity_requests 单表，满足「一个展示字段只来源一张表」。
export interface RequirementDTO {
  request_id: string
  title: string
  description: string
  industry: string
  urgency: string // normal | urgent | critical
  status: string // Pending | Collecting | Adopted | Closed
  publisher_name: string
  publisher_dept_name: string
  category_names: string[]
  visibility_type: string // all | dept | personnel
  visibility_values?: string[]
  invited_product_line_names: string[]
  invited_product_lines?: InvitedProductLineDTO[]
  sla_status: string // normal | warning | overdue | responded
  escalation_level: string // L0 | L1 | L2 | L3
  is_pinned: boolean
  cover_url?: string
  view_count: number
  response_count: number
  like_count?: number
  collect_count?: number
  adopted_response_id: string | null
  responses?: RequirementResponseDTO[]
  created_at: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface RequirementItem {
  id: string
  title: string
  description: string
  industry: string
  urgency: string
  status: string
  publisherName: string
  publisherDeptName: string
  categoryNames: string[]
  visibilityType: string
  visibilityValues: string[]
  invitedProductLineNames: string[]
  invitedProductLines: InvitedProductLineItem[]
  slaStatus: string
  escalationLevel: string
  isPinned: boolean
  coverUrl: string
  viewCount: number
  responseCount: number
  likeCount: number
  collectCount: number
  adoptedResponseId: string
  responses: RequirementResponseItem[]
  createdAt: string
}

// ============ 分页 ============
export type RequirementPageParams = PaginationParams<{
  keyword?: string
  urgency?: string
  status?: string
  sort?: string
}>
export type RequirementPageResult = PaginationResult<RequirementItem>

// ============ 增删改参数（用 type 别名，满足 request BodyArg 的 Record<string,JsonValue> 索引签名）============
export type RequirementCreateParams = {
  publisherId?: string
  title: string
  urgency: string
  industry?: string
  keywords?: string[]
  categoryIds: string[]
  visibilityType: string
  description: string
}
export type RequirementUpdateParams = RequirementCreateParams & {
  id: string
}
