package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 讨论区递归回帖节点出参。与前端 {@code CommentDTO}（discussion/types.ts）契约逐字对齐：
 * {@code comment_id / author_name / content / created_at / children[]}。
 *
 * <p>刻意与 interaction 模块的 {@link CommentVO} 分开命名——两者字段完全不同
 * （interaction 是 ≤2 级 + interaction_id/targetXxx/like_count/replies；讨论区是不限层级
 * 自引用树 + comment_id/children），是两套独立契约，不复用不继承。</p>
 *
 * <p>类级 @JsonNaming 出 snake_case；children 自嵌套，深度不限（讨论区不受 MOD-04
 * 「评论 ≤ 2 级」约束，该差异见 DiscussionServiceImpl javadoc）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DiscussionCommentVO {

    /** 前端 adapter 读的键名是 comment_id（Long→String 全局序列化成字符串） */
    private Long commentId;

    private String authorName;

    private String content;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 Date.parse((s).replace(' ','T')) 解析异常 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 子回帖，按 create_time 正序；无子节点时为空数组而非 null（前端 mapComments 递归读取） */
    private List<DiscussionCommentVO> children = new ArrayList<>();
}
