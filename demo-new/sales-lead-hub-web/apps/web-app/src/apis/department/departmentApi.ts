import { request } from '@q-web-plugin/request'

/** 部门树节点（a-tree-select tree-data 契约：{ value, label, children }） */
export interface DeptTreeNode {
  value: string
  label: string
  children?: DeptTreeNode[]
}

// 后端 GET department/tree 出参：[{ department_id, name, children[] }]
interface DeptDTO {
  department_id: string
  name: string
  children?: DeptDTO[]
}

const toNode = (dto: DeptDTO): DeptTreeNode => {
  const children = (dto.children ?? []).map(toNode)
  // 无子节点不挂 children 键（挂空数组会让 tree-select 渲染多余展开箭头）
  return children.length > 0
    ? { value: String(dto.department_id), label: dto.name ?? '', children }
    : { value: String(dto.department_id), label: dto.name ?? '' }
}

/**
 * 拉取部门树（可见性配置的按部门选择用）。
 * 取代原先表单硬编码/JSON mock 的 deptTree——树的 SSOT 是后端 department 表。
 */
export const fetchDepartmentTree = async (): Promise<DeptTreeNode[]> => {
  const raw = await request.GET<DeptDTO[]>({ url: 'department/tree' })
  return ((raw as DeptDTO[] | null) ?? []).map(toNode)
}
