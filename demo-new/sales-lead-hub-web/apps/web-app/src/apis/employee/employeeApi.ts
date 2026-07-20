import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getEmployeePageAdapter } from './employeeAdapter'
import type { EmployeePageParams, EmployeePageResult } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/**
 * 分页查询人员（选人器数据源，keyword 模糊匹配姓名/工号）。
 * real-only：无 mockRequest 包裹，Guard 始终 real 模式直发 employee/page。
 */
export const getEmployeePage = async (params: EmployeePageParams): Promise<EmployeePageResult> => {
  return (await AIRequestGuard({
    adapter: getEmployeePageAdapter,
    request: () => request.POST<EmployeePageResult>({ url: 'employee/page' }, params)
  })) as EmployeePageResult
}
