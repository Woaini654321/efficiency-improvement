import AIRequestGuard from '@ai-request-guard/core'
import type {
  RequirementDTO,
  RequirementItem,
  RequirementPageResult,
  RequirementResponseDTO,
  RequirementResponseItem
} from './types'
import mockData from './mocks/requirement.json'

// 方案响应映射
const mapResponses = (list: RequirementResponseDTO[] | undefined): RequirementResponseItem[] =>
  (list ?? []).map((r, i) => ({
    id: String(r.response_id ?? i),
    responderName: r.responder_name ?? '',
    responderDeptName: r.responder_dept_name ?? '',
    content: r.content ?? '',
    isAdopted: r.is_adopted ?? false,
    createdAt: r.created_at ?? ''
  }))

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染，双语）
const toItem = (dto: RequirementDTO): RequirementItem => ({
  id: String(dto.request_id),
  title: dto.title ?? '',
  description: dto.description ?? '',
  industry: dto.industry ?? '',
  urgency: dto.urgency ?? '',
  status: dto.status ?? '',
  publisherName: dto.publisher_name ?? '',
  publisherDeptName: dto.publisher_dept_name ?? '',
  categoryNames: dto.category_names ?? [],
  visibilityType: dto.visibility_type ?? '',
  invitedProductLineNames: dto.invited_product_line_names ?? [],
  slaStatus: dto.sla_status ?? '',
  escalationLevel: dto.escalation_level ?? '',
  isPinned: dto.is_pinned ?? false,
  viewCount: dto.view_count ?? 0,
  responseCount: dto.response_count ?? 0,
  adoptedResponseId: dto.adopted_response_id ?? '',
  responses: mapResponses(dto.responses),
  createdAt: dto.created_at ?? ''
})

// ============ 分页列表 adapter ============
export const getRequirementListAdapter = (raw: unknown): RequirementPageResult => {
  const data = raw as { records: RequirementDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    total: data.total ?? 0
  }
}

// ============ 详情 adapter ============
export const getRequirementDetailAdapter = (raw: unknown): RequirementItem => toItem(raw as RequirementDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () =>
    getRequirementListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getRequirementListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getRequirementDetailAdapter(mockData.records[0]),
  adapter: getRequirementDetailAdapter
})
