package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求详情出参 = 列表项字段集 + updatedAt + version。
 *
 * <p><b>刻意不继承 {@link RequirementPageVO}</b>：契约类型保持扁平可读，
 * 字段增减时编译器能在 convert 里直接报错，避免继承链掩盖契约漂移。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequirementDetailVO {

    private Long requestId;

    private String title;

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

    private String adoptedResponseId;

    private String slaStatus;

    /** L0 / L1 / L2 / L3 */
    private String escalationLevel;

    private Integer responseCount;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private Integer commentCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /** 供前端 update 时原样回传，乐观锁依赖它。 */
    private Integer version;
}
