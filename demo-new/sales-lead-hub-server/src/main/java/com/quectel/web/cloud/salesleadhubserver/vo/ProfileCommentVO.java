package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人中心-我的评论项。键对齐前端 adapter：
 * comment_id/content/source_title/source_type/source_id/is_deleted/created_at。
 *
 * <p>只取未软删评论（content_deleted=0）；来源目标回查不到（逻辑删/物理删）即丢弃，故 is_deleted 恒 false。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileCommentVO {

    /** 前端读 comment_id */
    private Long commentId;

    private String content;

    private String sourceTitle;

    /** opportunity / requirement */
    private String sourceType;

    private Long sourceId;

    private Boolean isDeleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
