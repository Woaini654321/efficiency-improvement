package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 运营看板·页面热力项。
 *
 * <p><b>近似口径</b>：平台无页面级埋点表，用 view_log 的 target_type 分布近似映射为
 * 「商机详情 / 需求详情 / 其他」，精确页面埋点属下期（javadoc 记录，不假装精确）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardPageHeatVO {

    /** 页面名（商机详情/需求详情/其他） */
    private String page;

    private Integer count;

    /** 占比（%，保留 1 位小数） */
    private Double percent;
}
