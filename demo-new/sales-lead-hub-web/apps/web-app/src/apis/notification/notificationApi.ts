import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getNotificationListAdapter } from './notificationAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/notification.json'
import type {
  NotificationPageParams,
  NotificationPageResult,
  NotificationPreferenceParams
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询通知列表 */
export const getNotificationList = async (
  params: NotificationPageParams
): Promise<NotificationPageResult> => {
  return (await AIRequestGuard({
    adapter: getNotificationListAdapter,
    request: mockRequest('notification',
      { records: mockData.records, total: mockData.total },
      () => request.POST<NotificationPageResult>({ url: 'notification/page' }, params)
    )
  })) as NotificationPageResult
}

// ============ 增删改（直接 request）============

/** 标记单条通知已读 */
export const markNotificationRead = async (id: string): Promise<void> => {
  await request.POST({ url: 'notification/markRead' }, { id })
}

/** 全部标记已读 */
export const markAllNotificationRead = async (): Promise<void> => {
  await request.POST({ url: 'notification/markAllRead' }, {})
}

/** 保存通知偏好矩阵 */
export const saveNotificationPreference = async (
  params: NotificationPreferenceParams
): Promise<void> => {
  await request.POST({ url: 'notification/preference/save' }, params)
}
