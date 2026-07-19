import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getProfileAdapter } from './profileAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/profile.json'
import type { ProfileAggregate } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 查询个人中心聚合数据（用户信息 + 统计 + 各列表） */
export const getProfile = async (): Promise<ProfileAggregate> => {
  return (await AIRequestGuard({
    adapter: getProfileAdapter,
    request: mockRequest(
      mockData,
      () => request.GET<ProfileAggregate>({ url: 'profile/center' }, {})
    )
  })) as ProfileAggregate
}
