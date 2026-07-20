import AIRequestGuard from '@ai-request-guard/core'
import type { CategoryDTO, CategoryItem, CategoryPageResult } from './types'
import mockData from './mocks/category.json'

// 单条 DTO → ViewModel
const toItem = (dto: CategoryDTO): CategoryItem => ({
  id: String(dto.category_id),
  name: dto.name ?? '',
  nameEn: dto.name_en ?? '',
  parentId: dto.parent_id != null ? String(dto.parent_id) : '',
  sortOrder: dto.sort_order ?? 0,
  isActive: dto.is_active ?? false,
  contentCount: dto.content_count ?? 0
})

// ============ 列表 adapter（扁平列表，页面自行 buildTree）============
export const getCategoryListAdapter = (raw: unknown): CategoryPageResult => {
  const data = raw as { records: CategoryDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    // 后端全局 Long→String 会把分页 total 也序列化成字符串（实测 "total":"1"），强制收敛为 number
    total: Number(data.total ?? 0)
  }
}

// ============ 详情 adapter ============
export const getCategoryDetailAdapter = (raw: unknown): CategoryItem => toItem(raw as CategoryDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getCategoryListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getCategoryListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getCategoryDetailAdapter(mockData.records[0]),
  adapter: getCategoryDetailAdapter
})
