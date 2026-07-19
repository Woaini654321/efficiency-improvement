import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getDashboardAdapter } from './dashboardAdapter'
import type { DashboardData, DashboardQueryParams } from './types'

/** 查询运营数据看板聚合数据 */
export const getDashboard = async (params: DashboardQueryParams = {}): Promise<DashboardData> => {
  return (await AIRequestGuard({
    adapter: getDashboardAdapter,
    request: () => request.POST<DashboardData>({ url: 'operation/dashboard' }, params)
  })) as DashboardData
}
