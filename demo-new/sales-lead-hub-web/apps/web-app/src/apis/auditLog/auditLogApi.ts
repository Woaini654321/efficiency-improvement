import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getAuditLogListAdapter } from './auditLogAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/auditLog.json'
import type { AuditLogPageParams, AuditLogPageResult } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询操作日志 */
export const getAuditLogList = async (params: AuditLogPageParams): Promise<AuditLogPageResult> => {
  return (await AIRequestGuard({
    adapter: getAuditLogListAdapter,
    request: mockRequest('auditLog',
      { records: mockData.records, total: mockData.total },
      () => request.POST<AuditLogPageResult>({ url: 'auditLog/page' }, params)
    )
  })) as AuditLogPageResult
}
