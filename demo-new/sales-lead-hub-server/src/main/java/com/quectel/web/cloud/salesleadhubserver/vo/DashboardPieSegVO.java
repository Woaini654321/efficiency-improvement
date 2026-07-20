package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 运营看板·饼图扇区（商机/需求分类分布）。前端 adapter 把 name 读成 label。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardPieSegVO {

    private String name;

    private Integer value;

    /** 扇区颜色（后端固定色板轮询下发） */
    private String color;
}
