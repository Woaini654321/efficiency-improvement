import AIRequestGuard from '@ai-request-guard/core'
import type {
  SlaRequestDTO, SlaRequestItem, SlaPageResult, SlaStatsDTO, SlaStats,
  SlaMetaDTO, SlaMeta
} from './types'
import mockData from './mocks/sla.json'

const toItem = (dto: SlaRequestDTO): SlaRequestItem => ({
  id: String(dto.request_id),
  title: dto.title ?? '',
  urgency: dto.urgency ?? '',
  slaStatus: dto.sla_status ?? '',
  createdAt: dto.created_at ?? '',
  deadline: dto.deadline ?? '',
  remainingText: dto.remaining_text ?? '',
  responseCount: dto.response_count ?? 0,
  escalationLevel: dto.escalation_level ?? '',
  publisherName: dto.publisher_name ?? '',
  escalationTimeline: (dto.escalation_timeline ?? []).map((e) => ({
    time: e.time ?? '',
    desc: e.desc ?? '',
    notifyTo: e.notify_to ?? ''
  }))
})

// ============ 分页列表 adapter ============
export const getSlaListAdapter = (raw: unknown): SlaPageResult => {
  const data = raw as { records: SlaRequestDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    total: data.total ?? 0
  }
}

// ============ 统计卡 adapter ============
export const getSlaStatsAdapter = (raw: unknown): SlaStats => {
  const dto = (raw ?? {}) as SlaStatsDTO
  return {
    totalRequests: dto.total_requests ?? 0,
    timelyRate: dto.timely_rate ?? 0,
    respondedCount: dto.responded_count ?? 0,
    maxOverdueText: dto.max_overdue_text ?? ''
  }
}

// ============ 元数据 adapter（产品线负责人 / 邮件通知人）============
export const getSlaMetaAdapter = (raw: unknown): SlaMeta => {
  const dto = (raw ?? {}) as SlaMetaDTO
  return {
    productLeads: (dto.product_leads ?? []).map((l) => ({
      id: l.id ?? '', name: l.name ?? '', product: l.product ?? '', dept: l.dept ?? ''
    })),
    emailContacts: (dto.email_contacts ?? []).map((c) => ({ label: c.label ?? '', value: c.value ?? '' }))
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getSlaListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getSlaListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getSlaStatsAdapter(mockData.stats),
  adapter: getSlaStatsAdapter
})

AIRequestGuard.register({
  viewSchema: () => getSlaMetaAdapter(mockData.meta),
  adapter: getSlaMetaAdapter
})
