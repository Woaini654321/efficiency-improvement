import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
// audit_logs 单表：operator_name 为冗余快照字段；before/after 快照为 JSON 对象。
export interface AuditLogDTO {
  log_id: string
  operator_name: string
  action_type: string // publish|archive|delete|role_change|isolation_change|category_change|login|sla_escalation
  target: string
  result: string // success | failure
  ip_address: string
  user_agent: string
  before_snapshot: Record<string, unknown> | null
  after_snapshot: Record<string, unknown> | null
  created_at: string
}

// ============ ViewModel（前端视图类型，camelCase，ID 全 string）============
export interface AuditLogItem {
  id: string
  operatorName: string
  actionType: string
  target: string
  result: string
  ipAddress: string
  userAgent: string
  beforeSnapshot: Record<string, unknown> | null
  afterSnapshot: Record<string, unknown> | null
  createdAt: string
}

// ============ 分页 ============
export type AuditLogPageParams = PaginationParams<{
  keyword?: string
  actionType?: string
  result?: string
}>
export type AuditLogPageResult = PaginationResult<AuditLogItem>
