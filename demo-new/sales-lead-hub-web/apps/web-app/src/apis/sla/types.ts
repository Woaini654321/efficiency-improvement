import type { PaginationParams, PaginationResult } from '@q-mono-x/types/base'

// ============ DTO（后端原始类型，snake_case）============
// 需求时效监控视图（opportunity_requests SLA 视图），冗余快照字段落单表。
export interface SlaTimelineEventDTO {
  time: string
  desc: string
  notify_to: string
}
export interface SlaRequestDTO {
  request_id: string
  title: string
  urgency: string // normal | urgent | critical
  sla_status: string // normal | warning | overdue | responded
  created_at: string
  deadline: string
  remaining_text: string
  response_count: number
  escalation_level: string // L0 | L1 | L2 | L3
  publisher_name: string
  escalation_timeline?: SlaTimelineEventDTO[]
}

export interface SlaStatsDTO {
  total_requests: number
  timely_rate: number
  responded_count: number
  max_overdue_text: string
}

export interface SlaProductLeadDTO {
  id: string
  name: string
  product: string
  dept: string
}
export interface SlaEmailContactDTO {
  label: string
  value: string
}
export interface SlaMetaDTO {
  product_leads: SlaProductLeadDTO[]
  email_contacts: SlaEmailContactDTO[]
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface SlaTimelineEvent {
  time: string
  desc: string
  notifyTo: string
}
export interface SlaRequestItem {
  id: string
  title: string
  urgency: string
  slaStatus: string
  createdAt: string
  deadline: string
  remainingText: string
  responseCount: number
  escalationLevel: string
  publisherName: string
  escalationTimeline: SlaTimelineEvent[]
}

export interface SlaStats {
  totalRequests: number
  timelyRate: number
  respondedCount: number
  maxOverdueText: string
}

export interface SlaProductLead {
  id: string
  name: string
  product: string
  dept: string
}
export interface SlaEmailContact {
  label: string
  value: string
}
export interface SlaMeta {
  productLeads: SlaProductLead[]
  emailContacts: SlaEmailContact[]
}

// ============ 分页 ============
export type SlaPageParams = PaginationParams<{
  urgency?: string
  slaStatus?: string
  startDate?: string
  endDate?: string
}>
export type SlaPageResult = PaginationResult<SlaRequestItem>

// ============ 催办参数 ============
export type SlaUrgeParams = {
  id: string
  targets: string[]
  methods: string[]
  remark?: string
}
