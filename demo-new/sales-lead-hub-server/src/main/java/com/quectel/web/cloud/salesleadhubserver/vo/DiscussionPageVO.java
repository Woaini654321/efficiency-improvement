package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 讨论帖列表项出参。类级 @JsonNaming 出 snake_case（禁全局策略，理由见 OpportunityPageVO）。
 *
 * <p>键名对齐前端 {@code DiscussionDTO}：post_id/title/content/topic/author_name/
 * reply_count/view_count/is_hot/created_at/tags[]。列表页也要展示 content 摘要行，
 * 故与 detail 一样下发 content（讨论帖正文比商机短，不单独砍）。</p>
 *
 * <p>与 {@link DiscussionDetailVO} 刻意不互相继承——扁平契约类型，任一侧增减字段
 * 编译器能直接指到。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DiscussionPageVO {

    /** 前端 adapter 读的键名是 post_id（不是 id） */
    private Long postId;

    private String title;

    private String content;

    /** business/solution/experience/industry/complaint */
    private String topic;

    private String authorName;

    private Integer replyCount;

    private Integer viewCount;

    private Boolean isHot;

    private List<String> tags;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
