import { getCategoryList } from './categoryApi'
import type { CategoryItem } from './types'

/** 级联选择器节点（a-cascader options 契约） */
export interface CategoryTreeNode {
  value: string
  label: string
  children?: CategoryTreeNode[]
}

/**
 * 扁平分类列表 → 级联树。
 *
 * - 停用（isActive=false）的分类整枝过滤——与 DB 种子/运营停用语义一致；
 * - 无子节点不挂 children 键（挂空数组会让 cascader 渲染可展开箭头）；
 * - 排序遵循后端 parent_id + sort_order 顺序，此处不重排。
 */
export function buildCategoryTree(items: CategoryItem[]): CategoryTreeNode[] {
  const active = items.filter((i) => i.isActive)
  const byParent = new Map<string, CategoryItem[]>()
  for (const item of active) {
    const key = item.parentId || ''
    const list = byParent.get(key) ?? []
    list.push(item)
    byParent.set(key, list)
  }
  const build = (parentId: string): CategoryTreeNode[] =>
    (byParent.get(parentId) ?? []).map((i) => {
      const children = build(i.id)
      return children.length > 0
        ? { value: i.id, label: i.name, children }
        : { value: i.id, label: i.name }
    })
  return build('')
}

/**
 * 拉取启用中的分类树（发布表单选项用）。
 * 取代原先各表单硬编码/JSON mock 的分类树——树的 SSOT 是 DB category 表。
 */
export async function fetchCategoryTree(): Promise<CategoryTreeNode[]> {
  const res = await getCategoryList({ pageNumber: 1, pageSize: 500 })
  return buildCategoryTree(res.records)
}
