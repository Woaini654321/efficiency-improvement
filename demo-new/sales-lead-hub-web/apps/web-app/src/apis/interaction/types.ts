// ============ DTO（后端原始类型，snake_case）============
// 实体：interactions 表；评论 ≤ 2 级（PRD D7）。
// 说明：author_name / author_dept 为冗余快照字段，落在 interactions 单表，
// 满足「一个展示字段只来源一张表」。replies 仅一层，回复的 replies 恒为空。
export interface CommentDTO {
  interaction_id: string
  target_type: string // Opportunity | Request
  target_id: string
  author_name: string
  author_dept: string
  content: string
  like_count: number
  parent_id?: string | null
  created_at: string
  replies?: CommentDTO[]
}

// ============ ViewModel（前端视图类型，camelCase）============
// 二级评论：顶级评论含 replies；回复项 replies 恒为 []（不再嵌套）。
export interface Comment {
  id: string
  authorName: string
  authorDept: string
  content: string
  likeCount: number
  createdAt: string
  replies: Comment[]
}

// ============ 查询参数 ============
export type CommentQueryParams = {
  targetType: string
  targetId: string
}

// ============ 增改参数（用 type 别名，满足 request BodyArg 的 Record<string,JsonValue> 索引签名）============
export type AddCommentParams = {
  targetType: string
  targetId: string
  content: string
  parentId?: string
}

// 对内容主体（商机/需求）点赞或收藏的切换入参：
// id=目标 id；targetType='Opportunity'|'Request'；type='like'|'collect'
export type LikeTargetParams = {
  id: string
  targetType: string
  type: string
}
