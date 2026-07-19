import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getCategoryListAdapter } from './categoryAdapter'
import type {
  CategoryPageParams,
  CategoryPageResult,
  CategoryCreateParams,
  CategoryUpdateParams,
  CategoryActiveParams
} from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 查询分类列表（扁平，按字典类型过滤） */
export const getCategoryList = async (params: CategoryPageParams): Promise<CategoryPageResult> => {
  return (await AIRequestGuard({
    adapter: getCategoryListAdapter,
    request: () => request.POST<CategoryPageResult>({ url: 'category/list' }, params)
  })) as CategoryPageResult
}

// ============ 增删改（直接 request）============

/** 新建分类 */
export const createCategory = async (params: CategoryCreateParams): Promise<void> => {
  await request.POST({ url: 'category/create' }, params)
}

/** 更新分类 */
export const updateCategory = async (params: CategoryUpdateParams): Promise<void> => {
  await request.POST({ url: 'category/update' }, params)
}

/** 停用 / 启用分类 */
export const changeCategoryActive = async (params: CategoryActiveParams): Promise<void> => {
  await request.POST({ url: 'category/changeActive' }, params)
}

/** 删除分类 */
export const deleteCategory = async (id: string): Promise<void> => {
  await request.POST({ url: 'category/delete' }, { id })
}
