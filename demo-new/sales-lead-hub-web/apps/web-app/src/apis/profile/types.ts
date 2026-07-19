// ============ 个人中心聚合 DTO（后端原始类型，snake_case）============
// 说明：各展示字段均取自各自实体表的冗余快照列，前端只做扁平映射，绝不跨表拼装。
export interface ProfileUserDTO {
  name: string
  dept_name: string
  role_name: string
  employee_no: string
}
export interface ProfileStatsDTO {
  collect_count: number
  comment_count: number
  publish_count: number
  solution_count: number
  draft_count: number
  view_count: number
}
export interface ProfileCollectDTO {
  collect_id: string
  title: string
  type: string // opportunity | requirement
  is_deleted: boolean
  created_at: string
}
export interface ProfilePublishDTO {
  opportunity_id: string
  title: string
  status: string // draft | published | archived
  view_count: number
  like_count: number
  comment_count: number
  created_at: string
}
export interface ProfileSolutionDTO {
  solution_id: string
  title: string
  request_title: string
  summary: string
  adopter_name: string
  adopter_dept_name: string
}
export interface ProfileCommentDTO {
  comment_id: string
  content: string
  source_title: string
  created_at: string
}
export interface ProfileHistoryDTO {
  history_id: string
  title: string
  type: string
  viewed_at: string
}
export interface ProfileDTO {
  user: ProfileUserDTO
  stats: ProfileStatsDTO
  subscriptions: string[]
  collects: ProfileCollectDTO[]
  publishes: ProfilePublishDTO[]
  solutions: ProfileSolutionDTO[]
  comments: ProfileCommentDTO[]
  view_history: ProfileHistoryDTO[]
}

// ============ 个人中心聚合 ViewModel（前端视图类型，camelCase）============
export interface ProfileUser {
  name: string
  deptName: string
  roleName: string
  employeeNo: string
}
export interface ProfileStats {
  collectCount: number
  commentCount: number
  publishCount: number
  solutionCount: number
  draftCount: number
  viewCount: number
}
export interface ProfileCollect {
  id: string
  title: string
  type: string
  isDeleted: boolean
  createdAt: string
}
export interface ProfilePublish {
  id: string
  title: string
  status: string
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: string
}
export interface ProfileSolution {
  id: string
  title: string
  requestTitle: string
  summary: string
  adopterName: string
  adopterDeptName: string
}
export interface ProfileComment {
  id: string
  content: string
  sourceTitle: string
  createdAt: string
}
export interface ProfileHistory {
  id: string
  title: string
  type: string
  viewedAt: string
}
export interface ProfileAggregate {
  user: ProfileUser
  stats: ProfileStats
  subscriptions: string[]
  collects: ProfileCollect[]
  publishes: ProfilePublish[]
  solutions: ProfileSolution[]
  comments: ProfileComment[]
  viewHistory: ProfileHistory[]
}
