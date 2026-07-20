package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商机列表项出参。类级 @JsonNaming 出 snake_case（禁全局策略，理由见 RequirementPageVO）。
 *
 * <p>schema 无 cover_url/is_pinned/published_at/expiry_date/superseded_by 列，
 * VO 不声明这些键——前端 adapter 对每个键都有 {@code ??} 默认值兜底，缺键安全落默认。
 * 不下发 content：列表页不读，MEDIUMTEXT 白拖带宽。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpportunityPageVO {

    /** 前端 adapter 读的键名是 opportunity_id（不是 id） */
    private Long opportunityId;

    private String title;

    private String summary;

    /** product_info / solution / success_case */
    private String type;

    /** draft / published / archived */
    private String status;

    private String publisherName;

    private String publisherDeptName;

    private List<String> categoryNames;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private Integer commentCount;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
