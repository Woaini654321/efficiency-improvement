import { request } from '@q-web-plugin/request'
import AIRequestGuard from '@ai-request-guard/core'
import { getAuditListAdapter } from './auditAdapter'
import { mockRequest } from '../_shared/mock-switch'
import mockData from './mocks/audit.json'
import type { AuditPageParams, AuditPageResult, AuditStatusParams, AuditPinParams } from './types'

// ============ 查询类（AIRequestGuard 包裹）============

/** 分页查询待审核内容 */
export const getAuditList = async (params: AuditPageParams): Promise<AuditPageResult> => {
  return (await AIRequestGuard({
    adapter: getAuditListAdapter,
    request: mockRequest('audit',
      { records: mockData.records, total: mockData.total },
      () => request.POST<AuditPageResult>({ url: 'audit/page' }, params)
    )
  })) as AuditPageResult
}

// ============ 增删改（直接 request）============

/** 下架 / 恢复内容 */
export const changeAuditStatus = async (params: AuditStatusParams): Promise<void> => {
  await request.POST({ url: 'audit/changeStatus' }, params)
}

/** 置顶 / 取消置顶 */
export const changeAuditPin = async (params: AuditPinParams): Promise<void> => {
  await request.POST({ url: 'audit/changePin' }, params)
}

/** 删除内容 */
export const deleteAudit = async (id: string): Promise<void> => {
  await request.POST({ url: 'audit/delete' }, { id })
}
