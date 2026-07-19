import AIRequestGuard from '@ai-request-guard/core'
import type { ProfileDTO, ProfileAggregate } from './types'
import mockData from './mocks/profile.json'

// 聚合 DTO → ViewModel（枚举保留 code，标签在页面用 t('dict.*') 渲染，双语）
export const getProfileAdapter = (raw: unknown): ProfileAggregate => {
  const dto = raw as ProfileDTO
  const u = dto.user ?? ({} as ProfileDTO['user'])
  const s = dto.stats ?? ({} as ProfileDTO['stats'])
  return {
    user: {
      name: u.name ?? '',
      deptName: u.dept_name ?? '',
      roleName: u.role_name ?? '',
      employeeNo: u.employee_no ?? ''
    },
    stats: {
      collectCount: s.collect_count ?? 0,
      commentCount: s.comment_count ?? 0,
      publishCount: s.publish_count ?? 0,
      solutionCount: s.solution_count ?? 0,
      draftCount: s.draft_count ?? 0,
      viewCount: s.view_count ?? 0
    },
    subscriptions: dto.subscriptions ?? [],
    collects: (dto.collects ?? []).map((c) => ({
      id: String(c.collect_id),
      title: c.title ?? '',
      type: c.type ?? '',
      isDeleted: c.is_deleted ?? false,
      createdAt: c.created_at ?? ''
    })),
    publishes: (dto.publishes ?? []).map((p) => ({
      id: String(p.opportunity_id),
      title: p.title ?? '',
      status: p.status ?? '',
      viewCount: p.view_count ?? 0,
      likeCount: p.like_count ?? 0,
      commentCount: p.comment_count ?? 0,
      createdAt: p.created_at ?? ''
    })),
    solutions: (dto.solutions ?? []).map((sol) => ({
      id: String(sol.solution_id),
      title: sol.title ?? '',
      requestTitle: sol.request_title ?? '',
      summary: sol.summary ?? '',
      adopterName: sol.adopter_name ?? '',
      adopterDeptName: sol.adopter_dept_name ?? ''
    })),
    comments: (dto.comments ?? []).map((cm) => ({
      id: String(cm.comment_id),
      content: cm.content ?? '',
      sourceTitle: cm.source_title ?? '',
      createdAt: cm.created_at ?? ''
    })),
    viewHistory: (dto.view_history ?? []).map((h) => ({
      id: String(h.history_id),
      title: h.title ?? '',
      type: h.type ?? '',
      viewedAt: h.viewed_at ?? ''
    }))
  }
}

// ============ register ============
AIRequestGuard.register({
  viewSchema: () => getProfileAdapter(mockData),
  adapter: getProfileAdapter
})
