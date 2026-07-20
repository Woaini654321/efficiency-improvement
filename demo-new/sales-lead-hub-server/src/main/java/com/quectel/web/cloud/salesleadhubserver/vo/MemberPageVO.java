package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品线成员列表项出参。类级 @JsonNaming 出 snake_case（禁全局策略，理由见 OpportunityPageVO）。
 *
 * <p>member_id 是成员行主键（非 user_id）；product_line_name 由 service 从 product_line
 * 批量解析后回填（成员行本身不存该快照）。与 {@link MemberDetailVO} 刻意不互相继承。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MemberPageVO {

    /** 成员行主键 id */
    private Long memberId;

    private Long productLineId;

    /** 产品线名（service 回填，成员行不存该快照） */
    private String productLineName;

    private Long userId;

    /** 成员姓名快照 */
    private String userName;

    /** 1=产品线负责人(L1 升级人) */
    private Integer isOwner;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
