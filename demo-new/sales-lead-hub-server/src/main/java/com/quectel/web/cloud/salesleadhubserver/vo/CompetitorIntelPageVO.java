package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 竞品情报列表项出参。类级 @JsonNaming 出 snake_case。
 *
 * <p>与 {@link CompetitorIntelDetailVO} 刻意不互相继承——扁平契约类型，
 * 任一侧增减字段编译器能直接指到（理由见 RequirementDetailVO）。
 * 列表不下发 overview/analysis/impact/specs（MEDIUMTEXT/JSON 白拖带宽，
 * 前端 adapter 对缺键有 {@code ??} 兜底）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompetitorIntelPageVO {

    /** 前端 adapter 读的键名是 intel_id（不是 id） */
    private Long intelId;

    private String brand;

    private String product;

    /** new_product/price_change/customer_case/other */
    private String intelType;

    private String title;

    private String summary;

    private String source;

    private String submitterName;

    /** 列表卡片直接展示三计数：前端 adapter 读这三键，缺失会被 {@code ?? 0} 掩盖成恒 0 */
    private Integer likeCount;

    private Integer collectCount;

    private Integer viewCount;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
