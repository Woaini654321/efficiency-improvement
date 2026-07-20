package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人中心-我的发布项（商机 + 需求合并）。键对齐前端 adapter：
 * opportunity_id/title/type/status/view_count/like_count/comment_count/collect_count/
 * created_at/edited_at/is_adopted/replies[]。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfilePublishVO {

    /** 前端读 opportunity_id；商机/需求各自主键 */
    private Long opportunityId;

    private String title;

    /** opportunity / requirement */
    private String type;

    /** 商机：draft/published/archived；需求统一按 published 呈现（需求无草稿态） */
    private String status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 最后编辑时间（update_time） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime editedAt;

    /** 商机无采纳概念恒 false；需求=已选采纳方案（adopted_response_id 非空） */
    private Boolean isAdopted;

    private List<ProfilePublishReplyVO> replies;
}
