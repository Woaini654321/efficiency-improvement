import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// 情报规格键值对（关键信息一览）
export interface IntelSpec {
  label: string
  value: string
}

// ============ 竞品情报 DTO（snake_case）============
export interface CompetitorIntelDTO {
  intel_id: string
  brand: string
  product: string
  intel_type: string // new_product | price_change | customer_case | other
  title: string
  summary: string
  source: string
  submitter_name: string
  created_at: string
  // 详情页正文（mock 展示字段）
  overview?: string
  specs?: IntelSpec[]
  analysis?: string
  impact?: string
  like_count?: number
  collect_count?: number
  view_count?: number
}
// 竞品情报 ViewModel（camelCase）
export interface CompetitorIntelItem {
  id: string
  brand: string
  product: string
  intelType: string
  title: string
  summary: string
  source: string
  submitterName: string
  createdAt: string
  overview: string
  specs: IntelSpec[]
  analysis: string
  impact: string
  likeCount: number
  collectCount: number
  viewCount: number
}

// ============ 行业情报 DTO（snake_case）============
export interface IndustryIntelDTO {
  intel_id: string
  industry: string
  title: string
  summary: string
  source: string
  created_at: string
  // 详情页正文（mock 展示字段）
  overview?: string
  key_points?: string[]
  analysis?: string
  impact?: string
  like_count?: number
  collect_count?: number
  view_count?: number
}
// 行业情报 ViewModel（camelCase）
export interface IndustryIntelItem {
  id: string
  industry: string
  title: string
  summary: string
  source: string
  createdAt: string
  overview: string
  keyPoints: string[]
  analysis: string
  impact: string
  likeCount: number
  collectCount: number
  viewCount: number
}

// ============ 分页 ============
export type CompetitorIntelPageParams = PaginationParams<{
  keyword?: string
  brand?: string
  intelType?: string
}>
export type CompetitorIntelPageResult = PaginationResult<CompetitorIntelItem>

export type IndustryIntelPageParams = PaginationParams<{
  keyword?: string
  industry?: string
}>
export type IndustryIntelPageResult = PaginationResult<IndustryIntelItem>

// ============ 提交参数（type 别名，满足 request BodyArg 索引签名）============
// intelType 为可选（D 决策：情报类型非必填）
export type CompetitorIntelSubmitParams = {
  brand: string
  product?: string
  intelType?: string
  source: string
  title: string
  content: string
}
