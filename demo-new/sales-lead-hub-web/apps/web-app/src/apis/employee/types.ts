import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端 employee/page 出参，snake_case）============
// EmployeePageVO 只下发四字段（最小暴露面，无 phone/email/avatar）。
export interface EmployeeDTO {
  id: string
  name: string
  employee_id: string
  department_name: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface EmployeeItem {
  id: string
  name: string
  employeeId: string
  departmentName: string
}

// ============ 分页 ============
export type EmployeePageParams = PaginationParams<{
  keyword?: string
}>
export type EmployeePageResult = PaginationResult<EmployeeItem>
