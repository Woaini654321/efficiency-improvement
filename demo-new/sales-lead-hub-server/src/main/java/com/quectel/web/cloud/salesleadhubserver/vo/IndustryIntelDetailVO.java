package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 行业情报详情出参。类级 @JsonNaming 出 snake_case。无 version（无编辑/乐观锁交互）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class IndustryIntelDetailVO {

    /** 前端 adapter 读的键名是 intel_id（不是 id） */
    private Long intelId;

    /** trend/automotive/policy/energy/industrial/smartcity */
    private String industry;

    private String title;

    private String summary;

    private String source;

    /** 详情-概述 */
    private String overview;

    /** 详情-分析 */
    private String analysis;

    /** 详情-影响 */
    private String impact;

    /** 要点列表 */
    private List<String> keyPoints;

    private Integer likeCount;

    private Integer collectCount;

    private Integer viewCount;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
