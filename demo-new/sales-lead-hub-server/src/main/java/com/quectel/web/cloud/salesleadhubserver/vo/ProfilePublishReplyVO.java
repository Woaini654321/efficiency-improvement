package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 我发布内容下的一条回复。键对齐前端：reply_id/content/from_name/replied_at。
 *
 * <p>数据源=该内容上的一级评论（interaction type=comment、content_deleted=0、parent_comment_id 为空）——
 * 是「他人对我发布内容的回应」的可靠服务端来源。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfilePublishReplyVO {

    /** 前端读 reply_id；用评论互动行 id */
    private Long replyId;

    private String content;

    /** 评论人姓名快照（interaction.user_name） */
    private String fromName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime repliedAt;
}
