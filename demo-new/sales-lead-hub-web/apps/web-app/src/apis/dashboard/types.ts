// ============ DTO（后端原始类型，snake_case）============
// 运营数据看板为聚合只读视图，各展示字段均为埋点/统计快照，单次聚合返回。
export interface DashboardHotContentDTO {
  content_id: string
  title: string
  type: string // product_info | solution | success_case
  view_count: number
}
export interface DashboardCategoryDistDTO {
  name: string
  count: number
  percent: number
}
export interface DashboardPageHeatDTO {
  page: string
  count: number
  percent: number
}
export interface DashboardPieSegDTO {
  name: string
  value: number
  color: string
}
export interface DashboardTrendMapDTO {
  last7d: number[]
  last4w: number[]
  last12w: number[]
  last6m: number[]
}
export interface DashboardDTO {
  uv: number
  pv: number
  uv_mom: number
  pv_mom: number
  active_users: number
  active_users_mom: number
  week_publish: number
  week_publish_mom: number
  response_rate: number
  response_rate_mom: number
  adopt_rate: number
  adopt_rate_mom: number
  hot_contents: DashboardHotContentDTO[]
  category_dist: DashboardCategoryDistDTO[]
  page_heat: DashboardPageHeatDTO[]
  week_publish_trend: DashboardTrendMapDTO
  response_rate_trend: DashboardTrendMapDTO
  opp_category_pie: DashboardPieSegDTO[]
  demand_category_pie: DashboardPieSegDTO[]
  hourly_active: number[]
  updated_at: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface DashboardHotContent {
  id: string
  title: string
  type: string
  viewCount: number
}
export interface DashboardCategoryDist {
  name: string
  count: number
  percent: number
}
export interface DashboardPageHeat {
  page: string
  count: number
  percent: number
}
export interface DashboardPieSeg {
  label: string
  value: number
  color: string
}
export interface DashboardTrendMap {
  last7d: number[]
  last4w: number[]
  last12w: number[]
  last6m: number[]
}
export interface DashboardData {
  uv: number
  pv: number
  uvMom: number
  pvMom: number
  activeUsers: number
  activeUsersMom: number
  weekPublish: number
  weekPublishMom: number
  responseRate: number
  responseRateMom: number
  adoptRate: number
  adoptRateMom: number
  hotContents: DashboardHotContent[]
  categoryDist: DashboardCategoryDist[]
  pageHeat: DashboardPageHeat[]
  weekPublishTrend: DashboardTrendMap
  responseRateTrend: DashboardTrendMap
  oppCategoryPie: DashboardPieSeg[]
  demandCategoryPie: DashboardPieSeg[]
  hourlyActive: number[]
  updatedAt: string
}

// ============ 查询参数 ============
export type DashboardQueryParams = {
  range?: string // last7d | last4w | last12w | last6m
}
