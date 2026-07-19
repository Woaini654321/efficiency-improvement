import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
// categories 单表：name / name_en 为本表字段，parent_id 自关联。
export interface CategoryDTO {
  category_id: string
  name: string
  name_en: string
  parent_id: string | null
  sort_order: number
  is_active: boolean
  content_count: number
}

// ============ ViewModel（前端视图类型，camelCase，ID 全 string）============
export interface CategoryItem {
  id: string
  name: string
  nameEn: string
  parentId: string
  sortOrder: number
  isActive: boolean
  contentCount: number
}

// ============ 分页 / 列表 ============
export type CategoryPageParams = PaginationParams<{
  keyword?: string
  dictType?: string
}>
export type CategoryPageResult = PaginationResult<CategoryItem>

// ============ 增删改参数（用 type 别名，满足 request BodyArg 索引签名）============
export type CategoryCreateParams = {
  name: string
  nameEn?: string | undefined
  parentId?: string | undefined
  sortOrder?: number | undefined
  isActive: boolean
  dictType?: string | undefined
}
export type CategoryUpdateParams = CategoryCreateParams & {
  id: string
}
export type CategoryActiveParams = {
  id: string
  isActive: boolean
}
