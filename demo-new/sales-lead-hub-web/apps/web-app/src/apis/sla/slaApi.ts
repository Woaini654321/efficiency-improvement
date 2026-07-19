import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getSlaListAdapter, getSlaStatsAdapter } from './slaAdapter'
import type { SlaPageParams, SlaPageResult, SlaStats, SlaUrgeParams } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询需求时效列表 */
export const getSlaList = async (params: SlaPageParams): Promise<SlaPageResult> => {
  return (await AIRequestGuard({
    adapter: getSlaListAdapter,
    request: () => request.POST<SlaPageResult>({ url: 'operation/sla/page' }, params)
  })) as SlaPageResult
}

/** 查询需求时效统计卡 */
export const getSlaStats = async (): Promise<SlaStats> => {
  return (await AIRequestGuard({
    adapter: getSlaStatsAdapter,
    request: () => request.GET<SlaStats>({ url: 'operation/sla/stats' })
  })) as SlaStats
}

// ============ 写操作（直接 request）============

/** 催办 */
export const urgeSlaRequest = async (params: SlaUrgeParams): Promise<void> => {
  await request.POST({ url: 'operation/sla/urge' }, params)
}
