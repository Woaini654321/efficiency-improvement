import AIRequestGuard from '@ai-request-guard/core'
import type { AnnouncementDTO, AnnouncementItem, AnnouncementPageResult } from './types'
import mockData from './mocks/announcement.json'

// 单条 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染，双语）
const toItem = (dto: AnnouncementDTO): AnnouncementItem => ({
  id: String(dto.announcement_id),
  title: dto.title ?? '',
  content: dto.content ?? '',
  type: dto.type ?? '',
  publisherName: dto.publisher_name ?? '',
  isPinned: dto.is_pinned ?? false,
  priority: dto.priority ?? '',
  status: dto.status ?? '',
  viewCount: dto.view_count ?? 0,
  publishedAt: dto.published_at ?? ''
})

// ============ 分页列表 adapter ============
export const getAnnouncementListAdapter = (raw: unknown): AnnouncementPageResult => {
  const data = raw as { records: AnnouncementDTO[]; total: number }
  const records = data.records ?? []
  return {
    records: records.map(toItem),
    // 后端全局 Long→String 会把分页 total 也序列化成字符串（实测 "total":"1"），强制收敛为 number
    total: Number(data.total ?? 0)
  }
}

// ============ 详情 adapter ============
export const getAnnouncementDetailAdapter = (raw: unknown): AnnouncementItem => toItem(raw as AnnouncementDTO)

// ============ register ============
AIRequestGuard.register({
  viewSchema: () =>
    getAnnouncementListAdapter({ records: mockData.records, total: mockData.total }),
  adapter: getAnnouncementListAdapter
})

AIRequestGuard.register({
  viewSchema: () => getAnnouncementDetailAdapter(mockData.records[0]),
  adapter: getAnnouncementDetailAdapter
})
