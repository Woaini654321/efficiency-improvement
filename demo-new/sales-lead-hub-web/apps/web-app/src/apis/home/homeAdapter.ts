import AIRequestGuard from '@ai-request-guard/core'
import type {
  HomeAnnouncementDTO,
  HomeDashboard,
  HomeDashboardDTO,
  HomePostDTO,
  HomeSolutionDTO,
  HomeTaskDTO
} from './types'
import mockData from './mocks/home.json'

const toTask = (dto: HomeTaskDTO) => ({
  id: String(dto.task_id),
  title: dto.title ?? '',
  meetingName: dto.meeting_name ?? '',
  deadline: dto.deadline ?? '',
  priority: dto.priority ?? 'normal',
  status: dto.status ?? 'pending',
  isOverdue: dto.is_overdue ?? false
})

const toSolution = (dto: HomeSolutionDTO) => ({
  id: String(dto.opportunity_id),
  rank: dto.rank ?? 0,
  title: dto.title ?? '',
  type: dto.type ?? '',
  viewCount: dto.view_count ?? 0,
  publisherName: dto.publisher_name ?? '',
  isSubscribed: dto.is_subscribed ?? false
})

const toAnnouncement = (dto: HomeAnnouncementDTO) => ({
  id: String(dto.announcement_id),
  title: dto.title ?? '',
  type: dto.type ?? 'other',
  publisherName: dto.publisher_name ?? '',
  publishedAt: dto.published_at ?? '',
  viewCount: dto.view_count ?? 0,
  isPinned: dto.is_pinned ?? false
})

const toPost = (dto: HomePostDTO) => ({
  id: String(dto.post_id),
  topic: dto.topic ?? '',
  title: dto.title ?? '',
  authorName: dto.author_name ?? '',
  replyCount: dto.reply_count ?? 0,
  viewCount: dto.view_count ?? 0,
  createdAt: dto.created_at ?? ''
})

// ============ 首页仪表盘聚合 adapter ============
export const getHomeDashboardAdapter = (raw: unknown): HomeDashboard => {
  const data = raw as HomeDashboardDTO
  const stats = data.stats ?? ({} as HomeDashboardDTO['stats'])
  return {
    stats: {
      solutionTotal: stats.solution_total ?? 0,
      pendingRequests: stats.pending_requests ?? 0,
      weekDiscussions: stats.week_discussions ?? 0,
      activeUsers: stats.active_users ?? 0
    },
    hotTags: data.hot_tags ?? [],
    quickTasks: (data.quick_tasks ?? []).map(toTask),
    hotSolutions: (data.hot_solutions ?? []).map(toSolution),
    announcements: (data.announcements ?? []).map(toAnnouncement),
    hotPosts: (data.hot_posts ?? []).map(toPost)
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getHomeDashboardAdapter(mockData),
  adapter: getHomeDashboardAdapter
})
