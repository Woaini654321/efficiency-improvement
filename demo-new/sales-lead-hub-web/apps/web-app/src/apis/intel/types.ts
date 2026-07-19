import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

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
}

// ============ 行业情报 DTO（snake_case）============
export interface IndustryIntelDTO {
  intel_id: string
  industry: string
  title: string
  summary: string
  source: string
  created_at: string
}
// 行业情报 ViewModel（camelCase）
export interface IndustryIntelItem {
  id: string
  industry: string
  title: string
  summary: string
  source: string
  createdAt: string
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
export type CompetitorIntelSubmitParams = {
  brand: string
  product?: string
  intelType: string
  source: string
  title: string
  content: string
}
