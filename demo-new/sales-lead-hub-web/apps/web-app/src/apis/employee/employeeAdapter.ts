import AIRequestGuard from '@ai-request-guard/core'
import type { EmployeeDTO, EmployeeItem, EmployeePageResult } from './types'

const toItem = (dto: EmployeeDTO): EmployeeItem => ({
  id: String(dto.id),
  name: dto.name ?? '',
  employeeId: dto.employee_id ?? '',
  departmentName: dto.department_name ?? ''
})

// ============ 分页列表 adapter ============
export const getEmployeePageAdapter = (raw: unknown): EmployeePageResult => {
  const data = raw as { records: EmployeeDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    // 后端全局 Long→String 会把分页 total 也序列化成字符串，强制收敛为 number
    total: Number(data.total ?? 0)
  }
}

// ============ register（人员选择器为 real-only，无 mock json，用内联 ViewModel 作 viewSchema）============
AIRequestGuard.register({
  viewSchema: () =>
    getEmployeePageAdapter({
      records: [{ id: '0', name: '', employee_id: '', department_name: '' }],
      total: 0
    }),
  adapter: getEmployeePageAdapter
})
