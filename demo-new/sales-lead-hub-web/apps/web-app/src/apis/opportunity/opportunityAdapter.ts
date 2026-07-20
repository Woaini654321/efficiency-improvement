import AIRequestGuard from '@ai-request-guard/core'
import type {
  AttachmentDTO,
  AttachmentItem,
  OpportunityDTO,
  OpportunityItem,
  OpportunityPageResult
} from './types'
import mockData from './mocks/opportunity.json'

// 附件映射
const mapAttachments = (list: AttachmentDTO[] | undefined): AttachmentItem[] =>
  (list ?? []).map((f, i) => ({
    uid: `${f.name}-${i}`,
    name: f.name ?? '',
    url: f.url ?? '',
    size: f.size ?? 0
  }))

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染，双语）
const toItem = (dto: OpportunityDTO): OpportunityItem => ({
  id: String(dto.opportunity_id),
  title: dto.title ?? '',
  summary: dto.summary ?? '',
  content: dto.content ?? '',
  type: dto.type ?? '',
  status: dto.status ?? '',
  publisherName: dto.publisher_name ?? '',
  publisherDeptName: dto.publisher_dept_name ?? '',
  categoryNames: dto.category_names ?? [],
  coverUrl: dto.cover_url ?? '',
  isPinned: dto.is_pinned ?? false,
  viewCount: dto.view_count ?? 0,
  likeCount: dto.like_count ?? 0,
  collectCount: dto.collect_count ?? 0,
  commentCount: dto.comment_count ?? 0,
  attachments: mapAttachments(dto.attachments),
  createdAt: dto.created_at ?? '',
  publishedAt: dto.published_at ?? '',
  expiryDate: dto.expiry_date ?? '',
  supersededBy: dto.superseded_by ?? '',
  version: dto.version ?? 0
})

// ============ 分页列表 adapter ============
export const getOpportunityListAdapter = (raw: unknown): OpportunityPageResult => {
  const data = raw as { records: OpportunityDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    // 后端全局 Long→String 会把分页 total 也序列化成字符串（实测 "total":"1"），强制收敛为 number
    total: Number(data.total ?? 0)
  }
}

// ============ 详情 adapter ============
export const getOpportunityDetailAdapter = (raw: unknown): OpportunityItem => toItem(raw as OpportunityDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () =>
    getOpportunityListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getOpportunityListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getOpportunityDetailAdapter(mockData.records[0]),
  adapter: getOpportunityDetailAdapter
})
