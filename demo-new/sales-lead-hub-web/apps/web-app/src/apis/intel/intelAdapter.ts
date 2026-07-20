import AIRequestGuard from '@ai-request-guard/core'
import type {
  CompetitorIntelDTO,
  CompetitorIntelItem,
  CompetitorIntelPageResult,
  IndustryIntelDTO,
  IndustryIntelItem,
  IndustryIntelPageResult
} from './types'
import mockData from './mocks/intel.json'

// ============ 竞品情报 映射 ============
const toCompetitorItem = (dto: CompetitorIntelDTO): CompetitorIntelItem => ({
  id: String(dto.intel_id),
  brand: dto.brand ?? '',
  product: dto.product ?? '',
  intelType: dto.intel_type ?? '',
  title: dto.title ?? '',
  summary: dto.summary ?? '',
  source: dto.source ?? '',
  submitterName: dto.submitter_name ?? '',
  createdAt: dto.created_at ?? '',
  overview: dto.overview ?? '',
  specs: dto.specs ?? [],
  analysis: dto.analysis ?? '',
  impact: dto.impact ?? '',
  likeCount: dto.like_count ?? 0,
  collectCount: dto.collect_count ?? 0,
  viewCount: dto.view_count ?? 0
})

export const getCompetitorListAdapter = (raw: unknown): CompetitorIntelPageResult => {
  const data = raw as { records: CompetitorIntelDTO[]; total: number }
  const records = data.records ?? []
  return { records: records.map(toCompetitorItem), total: Number(data.total ?? 0) }
}
export const getCompetitorDetailAdapter = (raw: unknown): CompetitorIntelItem =>
  toCompetitorItem(raw as CompetitorIntelDTO)

// ============ 行业情报 映射 ============
const toIndustryItem = (dto: IndustryIntelDTO): IndustryIntelItem => ({
  id: String(dto.intel_id),
  industry: dto.industry ?? '',
  title: dto.title ?? '',
  summary: dto.summary ?? '',
  source: dto.source ?? '',
  createdAt: dto.created_at ?? '',
  overview: dto.overview ?? '',
  keyPoints: dto.key_points ?? [],
  analysis: dto.analysis ?? '',
  impact: dto.impact ?? '',
  likeCount: dto.like_count ?? 0,
  collectCount: dto.collect_count ?? 0,
  viewCount: dto.view_count ?? 0
})

export const getIndustryListAdapter = (raw: unknown): IndustryIntelPageResult => {
  const data = raw as { records: IndustryIntelDTO[]; total: number }
  const records = data.records ?? []
  return { records: records.map(toIndustryItem), total: Number(data.total ?? 0) }
}
export const getIndustryDetailAdapter = (raw: unknown): IndustryIntelItem =>
  toIndustryItem(raw as IndustryIntelDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getCompetitorListAdapter({ records: mockData.competitors.records, total: mockData.competitors.total }),
  adapter: getCompetitorListAdapter
})
AIRequestGuard.register({
  viewSchema: () => getCompetitorDetailAdapter(mockData.competitors.records[0]),
  adapter: getCompetitorDetailAdapter
})
AIRequestGuard.register({
  viewSchema: () => getIndustryListAdapter({ records: mockData.industries.records, total: mockData.industries.total }),
  adapter: getIndustryListAdapter
})
AIRequestGuard.register({
  viewSchema: () => getIndustryDetailAdapter(mockData.industries.records[0]),
  adapter: getIndustryDetailAdapter
})
