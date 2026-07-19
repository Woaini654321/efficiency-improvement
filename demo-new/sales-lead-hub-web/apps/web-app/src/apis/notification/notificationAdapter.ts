import AIRequestGuard from '@ai-request-guard/core'
import type { NotificationDTO, NotificationItem, NotificationPageResult } from './types'
import mockData from './mocks/notification.json'

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染，双语）
const toItem = (dto: NotificationDTO): NotificationItem => ({
  id: String(dto.notification_id),
  type: dto.type ?? '',
  channel: dto.channel ?? '',
  title: dto.title ?? '',
  triggerUserName: dto.trigger_user_name ?? '',
  isRead: dto.is_read ?? false,
  isForceConfirm: dto.is_force_confirm ?? false,
  targetType: dto.target_type ?? '',
  targetId: String(dto.target_id ?? ''),
  createdAt: dto.created_at ?? ''
})

// ============ 分页列表 adapter ============
export const getNotificationListAdapter = (raw: unknown): NotificationPageResult => {
  const data = raw as { records: NotificationDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    total: data.total ?? 0
  }
}

// ============ 详情 adapter ============
export const getNotificationDetailAdapter = (raw: unknown): NotificationItem => toItem(raw as NotificationDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () =>
    getNotificationListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getNotificationListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getNotificationDetailAdapter(mockData.records[0]),
  adapter: getNotificationDetailAdapter
})
