import { request } from '@q-web-plugin/request'
import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'
import AIRequestGuard from '@ai-request-guard/core'
import { getUserPageAdapter } from './UserAdapter'

/**
 * 获取员工分页列表
 */
// 获取员工分页参数类型，这里基于 PaginationParams 基础的分页类型，扩展两个检索条件字段：key、type
type GetUserPageParams = PaginationParams<{
  key?: string // 关键字
}>
// 员工分页单条数据类型
export type GetUserPageRecord = {
  legalName: string
  enName: string
  staffNo: string
}

export type GetUserPageResult = PaginationResult<GetUserPageRecord>

export const getUserPage = async (params: GetUserPageParams): Promise<GetUserPageResult> => {
  return (await AIRequestGuard({
    adapter: getUserPageAdapter,
    request: () =>
      request.POST<GetUserPageResult>(
        {
          url: 'employee/page',
          urlType: 'org_url'
        },
        params
      )
  })) as GetUserPageResult
}
