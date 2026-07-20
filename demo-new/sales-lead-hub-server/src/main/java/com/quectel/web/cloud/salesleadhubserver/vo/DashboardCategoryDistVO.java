package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 运营看板·分类分布项（需求分类聚合）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardCategoryDistVO {

    private String name;

    private Integer count;

    /** 占比（%，保留 1 位小数） */
    private Double percent;
}
