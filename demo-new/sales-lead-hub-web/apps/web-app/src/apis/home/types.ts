// ============ DTO（后端原始类型，snake_case）============
// 说明：所有展示字段均为各自实体表的冗余快照列，满足「一个展示字段只来源一张表」。

export interface HomeStatsDTO {
  solution_total: number
  pending_requests: number
  week_discussions: number
  active_users: number
}

export interface HomeTaskDTO {
  task_id: string
  title: string
  meeting_name: string
  deadline: string
  priority: string // normal | urgent | critical
  status: string // pending | processing | completed | transferred | cancelled
  is_overdue: boolean
}

export interface HomeSolutionDTO {
  opportunity_id: string
  rank: number
  title: string
  type: string // product_info | solution | success_case
  view_count: number
  publisher_name: string
  is_subscribed: boolean
}

export interface HomeAnnouncementDTO {
  announcement_id: string
  title: string
  type: string // notice | policy | activity | other
  publisher_name: string
  published_at: string
  view_count: number
  is_pinned: boolean
}

export interface HomePostDTO {
  post_id: string
  topic: string // business | solution | experience | industry | complaint
  title: string
  author_name: string
  reply_count: number
  view_count: number
  created_at: string
}

export interface HomeDashboardDTO {
  stats: HomeStatsDTO
  hot_tags: string[]
  quick_tasks: HomeTaskDTO[]
  hot_solutions: HomeSolutionDTO[]
  announcements: HomeAnnouncementDTO[]
  hot_posts: HomePostDTO[]
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface HomeStats {
  solutionTotal: number
  pendingRequests: number
  weekDiscussions: number
  activeUsers: number
}

export interface HomeTask {
  id: string
  title: string
  meetingName: string
  deadline: string
  priority: string
  status: string
  isOverdue: boolean
}

export interface HomeSolution {
  id: string
  rank: number
  title: string
  type: string
  viewCount: number
  publisherName: string
  isSubscribed: boolean
}

export interface HomeAnnouncement {
  id: string
  title: string
  type: string
  publisherName: string
  publishedAt: string
  viewCount: number
  isPinned: boolean
}

export interface HomePost {
  id: string
  topic: string
  title: string
  authorName: string
  replyCount: number
  viewCount: number
  createdAt: string
}

export interface HomeDashboard {
  stats: HomeStats
  hotTags: string[]
  quickTasks: HomeTask[]
  hotSolutions: HomeSolution[]
  announcements: HomeAnnouncement[]
  hotPosts: HomePost[]
}
