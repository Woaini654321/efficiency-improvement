import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getAnnouncementListAdapter, getAnnouncementDetailAdapter } from './announcementAdapter'
import type {
  AnnouncementPageParams,
  AnnouncementPageResult,
  AnnouncementItem
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询公告列表 */
export const getAnnouncementList = async (
  params: AnnouncementPageParams
): Promise<AnnouncementPageResult> => {
  return (await AIRequestGuard({
    adapter: getAnnouncementListAdapter,
    request: () => request.POST<AnnouncementPageResult>({ url: 'announcement/page' }, params)
  })) as AnnouncementPageResult
}

/** 查询公告详情 */
export const getAnnouncementDetail = async (id: string): Promise<AnnouncementItem> => {
  return (await AIRequestGuard({
    adapter: getAnnouncementDetailAdapter,
    request: () => request.GET<AnnouncementItem>({ url: 'announcement/detail' }, { id })
  })) as AnnouncementItem
}
