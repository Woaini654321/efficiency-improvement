import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
// announcements 单表，publisher_name 为冗余快照列。
export interface AnnounceDTO {
  announcement_id: string
  title: string
  type: string // notice | policy | activity | other
  status: string // draft | published | archived
  priority: string // high | normal
  is_pinned: boolean
  publisher_name: string
  view_count: number
  created_at: string
  published_at?: string
  content?: string
  banner_enabled?: boolean
  // 乐观锁版本号：编辑回填后随 update 提交，后端 @NotNull 校验（不带会 400）
  version?: number
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
  // 乐观锁版本号，编辑提交时原样回传
  version: number
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
  // 后端 update 要求 @NotNull version，参与 WHERE version=? 乐观锁冲突检测
  version: number
}
