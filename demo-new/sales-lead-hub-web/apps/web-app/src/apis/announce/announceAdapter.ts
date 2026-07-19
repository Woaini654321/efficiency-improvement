import AIRequestGuard from '@ai-request-guard/core'
import type { AnnounceDTO, AnnounceItem, AnnouncePageResult, AnnounceStatsDTO, AnnounceStats } from './types'
import mockData from './mocks/announce.json'

const toItem = (dto: AnnounceDTO): AnnounceItem => ({
  id: String(dto.announcement_id),
  title: dto.title ?? '',
  type: dto.type ?? '',
  status: dto.status ?? '',
  priority: dto.priority ?? '',
  isPinned: dto.is_pinned ?? false,
  publisherName: dto.publisher_name ?? '',
  viewCount: dto.view_count ?? 0,
  createdAt: dto.created_at ?? '',
  publishedAt: dto.published_at ?? '',
  content: dto.content ?? '',
  bannerEnabled: dto.banner_enabled ?? false
})

// ============ 分页列表 adapter ============
export const getAnnounceListAdapter = (raw: unknown): AnnouncePageResult => {
  const data = raw as { records: AnnounceDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    total: data.total ?? 0
  }
}

// ============ 详情 adapter ============
export const getAnnounceDetailAdapter = (raw: unknown): AnnounceItem => toItem(raw as AnnounceDTO)

// ============ 统计卡 adapter ============
export const getAnnounceStatsAdapter = (raw: unknown): AnnounceStats => {
  const dto = (raw ?? {}) as AnnounceStatsDTO
  return {
    total: dto.total ?? 0,
    published: dto.published ?? 0,
    draft: dto.draft ?? 0,
    totalViews: dto.total_views ?? 0
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getAnnounceListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getAnnounceListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getAnnounceDetailAdapter(mockData.records[0]),
  adapter: getAnnounceDetailAdapter
})

AIRequestGuard.register({
  viewSchema: () => getAnnounceStatsAdapter(mockData.stats),
  adapter: getAnnounceStatsAdapter
})
