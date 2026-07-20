package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 需求时效统计卡出参。计数用 Integer/Double（不用 Long，避免全局 Long→String 把数字变字符串）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SlaStatsVO {

    /** 监控需求总数 */
    private Integer totalRequests;

    /** 及时率（未超时占比，%，保留 1 位小数） */
    private Double timelyRate;

    /** 已响应数 */
    private Integer respondedCount;

    /** 当前最长超时展示文案，如「26h」，无超时为空串 */
    private String maxOverdueText;
}
