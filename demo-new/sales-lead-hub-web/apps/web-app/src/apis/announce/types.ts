import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
// announcements 单表，publisher_name 为冗余快照列。
export interface AnnounceDTO {
  announcement_id: string
  title: string
  type: string // notice | policy | activity | other
  status: string // draft | published | expired
  priority: string // high | normal
  is_pinned: boolean
  publisher_name: string
  view_count: number
  created_at: string
  published_at?: string
  content?: string
  banner_enabled?: boolean
}

export interface AnnounceStatsDTO {
  total: number
  published: number
  draft: number
  total_views: number
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface AnnounceItem {
  id: string
  title: string
  type: string
  status: string
  priority: string
  isPinned: boolean
  publisherName: string
  viewCount: number
  createdAt: string
  publishedAt: string
  content: string
  bannerEnabled: boolean
}

export interface AnnounceStats {
  total: number
  published: number
  draft: number
  totalViews: number
}

// ============ 分页 ============
export type AnnouncePageParams = PaginationParams<{
  keyword?: string
  type?: string
  status?: string
}>
export type AnnouncePageResult = PaginationResult<AnnounceItem>

// ============ 增删改参数 ============
export type AnnounceCreateParams = {
  title: string
  type: string
  priority: string
  isPinned: boolean
  bannerEnabled: boolean
  content: string
}
export type AnnounceUpdateParams = AnnounceCreateParams & {
  id: string
}
