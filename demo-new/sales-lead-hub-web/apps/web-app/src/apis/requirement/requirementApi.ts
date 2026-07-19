import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getRequirementListAdapter, getRequirementDetailAdapter } from './requirementAdapter'
import type {
  RequirementPageParams,
  RequirementPageResult,
  RequirementItem,
  RequirementCreateParams,
  RequirementUpdateParams
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询需求列表 */
export const getRequirementList = async (
  params: RequirementPageParams
): Promise<RequirementPageResult> => {
  return (await AIRequestGuard({
    adapter: getRequirementListAdapter,
    request: () => request.POST<RequirementPageResult>({ url: 'requirement/page' }, params)
  })) as RequirementPageResult
}

/** 查询需求详情 */
export const getRequirementDetail = async (id: string): Promise<RequirementItem> => {
  return (await AIRequestGuard({
    adapter: getRequirementDetailAdapter,
    request: () => request.GET<RequirementItem>({ url: 'requirement/detail' }, { id })
  })) as RequirementItem
}

// ============ 增删改（直接 request）============

/** 创建需求 */
export const createRequirement = async (params: RequirementCreateParams): Promise<void> => {
  await request.POST({ url: 'requirement/create' }, params)
}

/** 更新需求 */
export const updateRequirement = async (params: RequirementUpdateParams): Promise<void> => {
  await request.POST({ url: 'requirement/update' }, params)
}
