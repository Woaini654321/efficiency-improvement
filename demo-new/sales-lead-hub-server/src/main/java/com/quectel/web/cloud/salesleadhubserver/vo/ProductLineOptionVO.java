package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 产品线选项出参。供需求发布表单的「邀请产品线」选择器使用。
 *
 * <p>类级 @JsonNaming 出 snake_case（{@code product_line_id / name}），禁全局策略
 * （双向会让入参 camelCase 静默变 null，理由见 OpportunityPageVO）。仅暴露 id 与 name
 * 两个选择器所需字段，description/version 等不下发（最小暴露面）。无时间字段，故无 @JsonFormat。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProductLineOptionVO {

    /** 产品线 id（Long→String 由全局序列化处理），序列化为 product_line_id */
    private Long productLineId;

    /** 产品线名 */
    private String name;
}
