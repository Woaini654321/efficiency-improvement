import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getRequirementListAdapter, getRequirementDetailAdapter } from './requirementAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/requirement.json'
import type {
  RequirementPageParams,
  RequirementPageResult,
  RequirementItem,
  RequirementCreateParams,
  RequirementUpdateParams,
  ResponseCreateParams,
  RequirementAdoptParams,
  RequirementCloseParams
} from './types'

/**
 * 本模块已切真实后端（'requirement' 不在 MOCK_MODULES 名单中，mockRequest 直通真实请求）。
 * 回退：把 'requirement' 加回 _shared/mock-switch.ts 的 MOCK_MODULES 即可（mock 文件未删除）。
 */

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询需求列表 */
export const getRequirementList = async (
  params: RequirementPageParams
): Promise<RequirementPageResult> => {
  return (await AIRequestGuard({
    adapter: getRequirementListAdapter,
    request: mockRequest(
      'requirement',
      { records: mockData.records, total: mockData.total },
      () => request.POST<RequirementPageResult>({ url: 'requirement/page' }, params)
    )
  })) as RequirementPageResult
}

/** 查询需求详情 */
export const getRequirementDetail = async (id: string): Promise<RequirementItem> => {
  return (await AIRequestGuard({
    adapter: getRequirementDetailAdapter,
    request: mockRequest(
      'requirement',
      mockData.records[0],
      () => request.GET<RequirementItem>({ url: 'requirement/detail' }, { id })
    )
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

/** 提交方案响应（POST response/create） */
export const submitResponse = async (params: ResponseCreateParams): Promise<void> => {
  await request.POST({ url: 'response/create' }, params)
}

/** 采纳方案为最佳（POST requirement/adopt） */
export const adoptResponse = async (params: RequirementAdoptParams): Promise<void> => {
  await request.POST({ url: 'requirement/adopt' }, params)
}

/** 关闭需求（POST requirement/close） */
export const closeRequirement = async (params: RequirementCloseParams): Promise<void> => {
  await request.POST({ url: 'requirement/close' }, params)
}
