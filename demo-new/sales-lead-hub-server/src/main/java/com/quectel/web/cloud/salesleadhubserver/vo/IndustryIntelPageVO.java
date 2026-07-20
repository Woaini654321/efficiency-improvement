package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行业情报列表项出参。类级 @JsonNaming 出 snake_case。
 *
 * <p>与 {@link IndustryIntelDetailVO} 刻意不互相继承（理由见 RequirementDetailVO）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndustryIntelPageVO {

    /** 前端 adapter 读的键名是 intel_id（不是 id） */
    private Long intelId;

    /** trend/automotive/policy/energy/industrial/smartcity */
    private String industry;

    private String title;

    private String summary;

    private String source;

    /** 列表卡片直接展示三计数：前端 adapter 读这三键，缺失会被 {@code ?? 0} 掩盖成恒 0 */
    private Integer likeCount;

    private Integer collectCount;

    private Integer viewCount;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
