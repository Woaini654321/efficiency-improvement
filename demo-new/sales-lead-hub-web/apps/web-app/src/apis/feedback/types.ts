// ============ DTO（后端原始类型，snake_case）============
// 匿名吐槽实体，字段均落在 feedback 单表（含 anon_name / like_count 冗余计数）。
export interface FeedbackDTO {
  feedback_id: string
  title: string
  content: string
  anon_name: string
  like_count: number
  created_at: string
  emoji: string
  color: string
}

// ============ ViewModel（前端视图类型，camelCase）============
export interface FeedbackItem {
  id: string
  title: string
  content: string
  anonName: string
  likeCount: number
  createdAt: string
  emoji: string
  color: string
}

export type FeedbackListResult = FeedbackItem[]

// ============ 新建参数（用 type 别名，满足 request BodyArg 索引签名）============
export type FeedbackCreateParams = {
  title: string
  content: string
}
