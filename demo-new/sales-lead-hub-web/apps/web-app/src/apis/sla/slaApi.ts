import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getSlaListAdapter, getSlaStatsAdapter, getSlaMetaAdapter } from './slaAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/sla.json'
import type { SlaPageParams, SlaPageResult, SlaStats, SlaUrgeParams, SlaMeta } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询需求时效列表 */
export const getSlaList = async (params: SlaPageParams): Promise<SlaPageResult> => {
  return (await AIRequestGuard({
    adapter: getSlaListAdapter,
    request: mockRequest(
      { records: mockData.records, total: mockData.total },
      () => request.POST<SlaPageResult>({ url: 'operation/sla/page' }, params)
    )
  })) as SlaPageResult
}

/** 查询需求时效统计卡 */
export const getSlaStats = async (): Promise<SlaStats> => {
  return (await AIRequestGuard({
    adapter: getSlaStatsAdapter,
    request: mockRequest(
      mockData.stats,
      () => request.GET<SlaStats>({ url: 'operation/sla/stats' })
    )
  })) as SlaStats
}

/** 查询催办元数据（产品线负责人 / 邮件通知人候选） */
export const getSlaMeta = async (): Promise<SlaMeta> => {
  return (await AIRequestGuard({
    adapter: getSlaMetaAdapter,
    request: mockRequest(
      mockData.meta,
      () => request.GET<SlaMeta>({ url: 'operation/sla/meta' })
    )
  })) as SlaMeta
}

// ============ 写操作（直接 request）============

/** 催办 */
export const urgeSlaRequest = async (params: SlaUrgeParams): Promise<void> => {
  await request.POST({ url: 'operation/sla/urge' }, params)
}
