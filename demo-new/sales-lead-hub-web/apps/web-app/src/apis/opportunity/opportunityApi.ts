import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getOpportunityListAdapter, getOpportunityDetailAdapter } from './opportunityAdapter'
import type {
  OpportunityPageParams,
  OpportunityPageResult,
  OpportunityItem,
  OpportunityCreateParams,
  OpportunityUpdateParams
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询方案列表 */
export const getOpportunityList = async (
  params: OpportunityPageParams
): Promise<OpportunityPageResult> => {
  return (await AIRequestGuard({
    adapter: getOpportunityListAdapter,
    request: () => request.POST<OpportunityPageResult>({ url: 'opportunity/page' }, params)
  })) as OpportunityPageResult
}

/** 查询方案详情 */
export const getOpportunityDetail = async (id: string): Promise<OpportunityItem> => {
  return (await AIRequestGuard({
    adapter: getOpportunityDetailAdapter,
    request: () => request.GET<OpportunityItem>({ url: 'opportunity/detail' }, { id })
  })) as OpportunityItem
}

// ============ 增删改（直接 request）============

/** 创建方案 */
export const createOpportunity = async (params: OpportunityCreateParams): Promise<void> => {
  await request.POST({ url: 'opportunity/create' }, params)
}

/** 更新方案 */
export const updateOpportunity = async (params: OpportunityUpdateParams): Promise<void> => {
  await request.POST({ url: 'opportunity/update' }, params)
}

/** 下架/恢复方案 */
export const changeOpportunityStatus = async (id: string, status: string): Promise<void> => {
  await request.POST({ url: 'opportunity/changeStatus' }, { id, status })
}

/** 删除方案 */
export const deleteOpportunity = async (id: string): Promise<void> => {
  await request.POST({ url: 'opportunity/delete' }, { id })
}
