package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 运营看板·热门内容项（view_count 前 5 商机）。嵌套 VO 自带 @JsonNaming（策略不级联）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardHotContentVO {

    /** 前端 adapter 读的键名是 content_id */
    private Long contentId;

    private String title;

    /** product_info/solution/success_case */
    private String type;

    private Integer viewCount;
}
