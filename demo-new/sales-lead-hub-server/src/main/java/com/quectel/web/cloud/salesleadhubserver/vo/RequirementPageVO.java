package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求列表项出参。
 *
 * <p>属性用 camelCase，经 {@code @JsonNaming} 序列化为 snake_case 与前端 adapter 对齐。
 * <b>刻意不用全局 PropertyNamingStrategy</b>：全局策略是双向的，会同时把入参
 * 也按 snake_case 解析，导致前端 camelCase payload 静默变 null，且会破坏
 * 框架 {@code /me} 的 {@code Result<User>} 契约与 MeControllerTest 基线。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequirementPageVO {

    private Long requestId;

    private String title;

    /** 列表卡片也读摘要，故列表项一并下发。 */
    private String description;

    private String industry;

    /** normal / urgent / critical */
    private String urgency;

    /** Pending / Collecting / Adopted / Closed */
    private String status;

    private String publisherName;

    private String publisherDeptName;

    private List<String> categoryNames;

    /** 对应 DO 的 visibilityScope，JSON 名必须是 visibility_type（前端实发字段名）。 */
    private String visibilityType;

    private List<String> visibilityValues;

    private List<String> invitedProductLineNames;

    /** 用 String，避免与全局 Long→String 序列化叠加产生歧义。 */
    private String adoptedResponseId;

    private String slaStatus;

    /** L0 / L1 / L2 / L3 */
    private String escalationLevel;

    private Integer responseCount;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private Integer commentCount;

    /**
     * 必须带 pattern：默认 ISO 的 'T' 分隔会让前端 {@code replace(/-/g,'/')} 解析出 NaN，
     * SLA 倒计时整块不显示。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
