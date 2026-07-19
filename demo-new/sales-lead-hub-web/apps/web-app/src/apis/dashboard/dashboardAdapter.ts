import AIRequestGuard from '@ai-request-guard/core'
import type {
  DashboardDTO,
  DashboardData,
  DashboardHotContentDTO,
  DashboardCategoryDistDTO,
  DashboardPageHeatDTO
} from './types'
import mockData from './mocks/dashboard.json'

const mapHot = (list: DashboardHotContentDTO[] | undefined) =>
  (list ?? []).map((d) => ({
    id: String(d.content_id),
    title: d.title ?? '',
    type: d.type ?? '',
    viewCount: d.view_count ?? 0
  }))

const mapCategory = (list: DashboardCategoryDistDTO[] | undefined) =>
  (list ?? []).map((d) => ({ name: d.name ?? '', count: d.count ?? 0, percent: d.percent ?? 0 }))

const mapPageHeat = (list: DashboardPageHeatDTO[] | undefined) =>
  (list ?? []).map((d) => ({ page: d.page ?? '', count: d.count ?? 0, percent: d.percent ?? 0 }))

// ============ 聚合 adapter（raw 已解包）============
export const getDashboardAdapter = (raw: unknown): DashboardData => {
  const dto = (raw ?? {}) as DashboardDTO
  return {
    uv: dto.uv ?? 0,
    pv: dto.pv ?? 0,
    uvMom: dto.uv_mom ?? 0,
    pvMom: dto.pv_mom ?? 0,
    activeUsers: dto.active_users ?? 0,
    activeUsersMom: dto.active_users_mom ?? 0,
    weekPublish: dto.week_publish ?? 0,
    weekPublishMom: dto.week_publish_mom ?? 0,
    responseRate: dto.response_rate ?? 0,
    responseRateMom: dto.response_rate_mom ?? 0,
    adoptRate: dto.adopt_rate ?? 0,
    adoptRateMom: dto.adopt_rate_mom ?? 0,
    hotContents: mapHot(dto.hot_contents),
    categoryDist: mapCategory(dto.category_dist),
    pageHeat: mapPageHeat(dto.page_heat),
    updatedAt: dto.updated_at ?? ''
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getDashboardAdapter(mockData),
  adapter: getDashboardAdapter
})
