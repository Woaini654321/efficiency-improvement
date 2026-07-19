import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getHomeDashboardAdapter } from './homeAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/home.json'
import type { HomeDashboard } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 查询首页工作台聚合数据 */
export const getHomeDashboard = async (): Promise<HomeDashboard> => {
  return (await AIRequestGuard({
    adapter: getHomeDashboardAdapter,
    request: mockRequest(
      mockData,
      () => request.GET<HomeDashboard>({ url: 'home/dashboard' })
    )
  })) as HomeDashboard
}
