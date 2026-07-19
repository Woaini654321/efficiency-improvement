import AIRequestGuard from '@ai-request-guard/core'
import type { ContentAuditDTO, AuditItem, AuditPageResult } from './types'
import mockData from './mocks/audit.json'

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染，双语）
const toItem = (dto: ContentAuditDTO): AuditItem => ({
  id: String(dto.audit_id),
  title: dto.title ?? '',
  contentType: dto.content_type ?? '',
  publisherName: dto.publisher_name ?? '',
  status: dto.status ?? '',
  isPinned: dto.is_pinned ?? false,
  publishedAt: dto.published_at ?? '',
  sortNo: dto.sort_no ?? 0,
  urgency: dto.urgency ?? 'normal',
  industry: dto.industry ?? '',
  tags: dto.tags ?? [],
  categoryPath: dto.category_path ?? [],
  description: dto.description ?? ''
})

// ============ 分页列表 adapter ============
export const getAuditListAdapter = (raw: unknown): AuditPageResult => {
  const data = raw as { records: ContentAuditDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    total: data.total ?? 0
  }
}

// ============ 详情 adapter ============
export const getAuditDetailAdapter = (raw: unknown): AuditItem => toItem(raw as ContentAuditDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getAuditListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getAuditListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getAuditDetailAdapter(mockData.records[0]),
  adapter: getAuditDetailAdapter
})
