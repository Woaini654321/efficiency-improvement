package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.quectel.web.cloud.salesleadhubserver.pojo.SpecItem;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞品情报详情出参。类级 @JsonNaming 出 snake_case。
 *
 * <p>无 version：竞品情报无编辑/乐观锁交互，计数列由原子自增维护。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompetitorIntelDetailVO {

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

    /** 详情-概述 */
    private String overview;

    /** 详情-分析 */
    private String analysis;

    /** 详情-影响 */
    private String impact;

    /** {label,value}，字段全小写单词，snake 化是 no-op */
    private List<SpecItem> specs;

    private Integer likeCount;

    private Integer collectCount;

    private Integer viewCount;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
