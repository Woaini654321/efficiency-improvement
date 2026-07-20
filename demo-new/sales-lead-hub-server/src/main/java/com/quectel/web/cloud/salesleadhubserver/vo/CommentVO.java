package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论出参（≤2 级树）。字段键名对齐前端 {@code CommentDTO}：
 * interaction_id / target_type / target_id / author_name / author_dept /
 * content / like_count / parent_id / created_at / replies。
 *
 * <p>类级 snake_case；Long 主键/外键由全局 Jackson {@code Long→String} 序列化成字符串，
 * 与前端 {@code String(dto.interaction_id)} 一致。一级评论含 replies；回复项 replies 恒为空。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommentVO {

    /** 前端读的是 interaction_id（不是 id） */
    private Long interactionId;

    /** Opportunity / Request / Response */
    private String targetType;

    private Long targetId;

    /** 评论人姓名快照（行内 user_name） */
    private String authorName;

    /** 评论人部门名（interaction 表无部门快照列，查询时按 user_id 联本地 sys_user 取） */
    private String authorDept;

    /** content_deleted=1 时输出占位文案「该评论已删除」，行与子回复仍保留 */
    private String content;

    /** 点赞数：按 target_type=Comment + target_id=本评论 + type=like 的行数派生 */
    private Integer likeCount;

    /** 父评论 id：一级评论为 null */
    private Long parentId;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 一层回复；回复项自身 replies 恒为空数组 */
    private List<CommentVO> replies;
}
