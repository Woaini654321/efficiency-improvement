import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getAnnounceListAdapter, getAnnounceDetailAdapter, getAnnounceStatsAdapter } from './announceAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/announce.json'
import type {
  AnnouncePageParams,
  AnnouncePageResult,
  AnnounceItem,
  AnnounceStats,
  AnnounceCreateParams,
  AnnounceUpdateParams
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询公告列表 */
export const getAnnounceList = async (params: AnnouncePageParams): Promise<AnnouncePageResult> => {
  return (await AIRequestGuard({
    adapter: getAnnounceListAdapter,
    request: mockRequest(
      { records: mockData.records, total: mockData.total },
      () => request.POST<AnnouncePageResult>({ url: 'operation/announce/page' }, params)
    )
  })) as AnnouncePageResult
}

/** 查询公告详情 */
export const getAnnounceDetail = async (id: string): Promise<AnnounceItem> => {
  return (await AIRequestGuard({
    adapter: getAnnounceDetailAdapter,
    request: mockRequest(
      mockData.records[0],
      () => request.GET<AnnounceItem>({ url: 'operation/announce/detail' }, { id })
    )
  })) as AnnounceItem
}

/** 查询公告统计卡 */
export const getAnnounceStats = async (): Promise<AnnounceStats> => {
  return (await AIRequestGuard({
    adapter: getAnnounceStatsAdapter,
    request: mockRequest(
      mockData.stats,
      () => request.GET<AnnounceStats>({ url: 'operation/announce/stats' })
    )
  })) as AnnounceStats
}

// ============ 增删改（直接 request）============

/** 创建公告 */
export const createAnnounce = async (params: AnnounceCreateParams): Promise<void> => {
  await request.POST({ url: 'operation/announce/create' }, params)
}

/** 更新公告 */
export const updateAnnounce = async (params: AnnounceUpdateParams): Promise<void> => {
  await request.POST({ url: 'operation/announce/update' }, params)
}

/** 变更公告状态（发布 / 下架 / 重新发布）*/
export const changeAnnounceStatus = async (id: string, status: string): Promise<void> => {
  await request.POST({ url: 'operation/announce/changeStatus' }, { id, status })
}

/** 删除公告 */
export const deleteAnnounce = async (id: string): Promise<void> => {
  await request.POST({ url: 'operation/announce/delete' }, { id })
}
