import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getHomeDashboardAdapter } from './homeAdapter'
import type { HomeDashboard } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 查询首页工作台聚合数据 */
export const getHomeDashboard = async (): Promise<HomeDashboard> => {
  return (await AIRequestGuard({
    adapter: getHomeDashboardAdapter,
    request: () => request.GET<HomeDashboard>({ url: 'home/dashboard' })
  })) as HomeDashboard
}
