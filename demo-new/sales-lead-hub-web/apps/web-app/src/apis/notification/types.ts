import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ 通知 DTO（后端原始类型，snake_case）============
// 说明：trigger_user_name 为冗余快照字段，落在 notifications 单表，满足「一个展示字段只来源一张表」。
export interface NotificationDTO {
  notification_id: string
  type: string // publish | response | adopt | system | mention | subscribe | force_confirm
  channel: string // in_app | feishu | email
  title: string
  trigger_user_name: string
  is_read: boolean
  is_force_confirm: boolean
  target_type: string // opportunity | requirement | announcement | ...
  target_id: string
  created_at: string
}

// ============ 通知 ViewModel（前端视图类型，camelCase）============
export interface NotificationItem {
  id: string
  type: string
  channel: string
  title: string
  triggerUserName: string
  isRead: boolean
  isForceConfirm: boolean
  targetType: string
  targetId: string
  createdAt: string
}

// ============ 公告 DTO（snake_case）============
export interface AnnouncementDTO {
  announcement_id: string
  title: string
  content: string
  type: string // notice | policy | activity | other
  publisher_name: string
  is_pinned: boolean
  priority: string // high | normal
  status: string // draft | published | expired
  view_count: number
  published_at?: string
}

// ============ 公告 ViewModel（camelCase）============
export interface AnnouncementItem {
  id: string
  title: string
  content: string
  type: string
  publisherName: string
  isPinned: boolean
  priority: string
  status: string
  viewCount: number
  publishedAt: string
}

// ============ 分页 ============
export type NotificationPageParams = PaginationParams<{
  keyword?: string
  type?: string
  isRead?: string
}>
export type NotificationPageResult = PaginationResult<NotificationItem>

export type AnnouncementPageParams = PaginationParams<{
  keyword?: string
  type?: string
}>
export type AnnouncementPageResult = PaginationResult<AnnouncementItem>

// ============ 通知偏好保存参数（用 type 别名，满足 request BodyArg 索引签名）============
export type NotificationPreferenceParams = {
  matrix: Record<string, Record<string, boolean>>
}
