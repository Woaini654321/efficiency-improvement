package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 首页「热门方案」项（view_count 倒序 Top5 已发布商机）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HomeSolutionVO {

    /** 前端 adapter 读的键名是 opportunity_id */
    private Long opportunityId;

    /** 榜单名次 1..N */
    private Integer rank;

    private String title;

    /** product_info/solution/success_case */
    private String type;

    private Integer viewCount;

    private String publisherName;

    /** 该商机任一分类命中当前用户订阅即 true */
    private Boolean isSubscribed;
}
