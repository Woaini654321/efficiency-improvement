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

/**
 * 本模块已切真实后端（竖切试点），**刻意不走 `_shared/mock-switch`**。
 *
 * 原因：那个开关是全局单布尔，20 个模块共用。把它置 false 会让其余 19 个
 * 尚无后端的模块同时打向不存在的接口、满屏报错，反而淹没 requirement 的联调信号。
 * 本模块单独直连，其余模块保持 mock 不受影响。
 *
 * 回退：恢复 `import { mockRequest } from '../_shared/mock-switch'` 与
 * `import mockData from './mocks/requirement.json'`，把下面两处 `request.*`
 * 包回 `mockRequest(<DTO 切片>, () => request.*)` 即可（mock 文件未删除）。
 * 待其余模块陆续接通后，再统一收敛回全局开关。
 */

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
