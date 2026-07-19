import AIRequestGuard from '@ai-request-guard/core'
import type { AuditLogDTO, AuditLogItem, AuditLogPageResult } from './types'
import mockData from './mocks/auditLog.json'

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染，双语）
const toItem = (dto: AuditLogDTO): AuditLogItem => ({
  id: String(dto.log_id),
  operatorName: dto.operator_name ?? '',
  actionType: dto.action_type ?? '',
  target: dto.target ?? '',
  result: dto.result ?? '',
  ipAddress: dto.ip_address ?? '',
  userAgent: dto.user_agent ?? '',
  beforeSnapshot: dto.before_snapshot ?? null,
  afterSnapshot: dto.after_snapshot ?? null,
  createdAt: dto.created_at ?? ''
})

// ============ 分页列表 adapter ============
export const getAuditLogListAdapter = (raw: unknown): AuditLogPageResult => {
  const data = raw as { records: AuditLogDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    total: data.total ?? 0
  }
}

// ============ 详情 adapter ============
export const getAuditLogDetailAdapter = (raw: unknown): AuditLogItem => toItem(raw as AuditLogDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getAuditLogListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getAuditLogListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getAuditLogDetailAdapter(mockData.records[0]),
  adapter: getAuditLogDetailAdapter
})
