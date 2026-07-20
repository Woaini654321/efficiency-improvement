package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

/**
 * 运营看板查询入参。字段以前端 {@code apis/dashboard/dashboardApi.ts} 实发为准：仅 {@code range?}。
 * camelCase 入参（全局未开 snake_case）。
 */
@Data
public class DashboardQueryDTO {

    /** 统计周期：last7d/last4w/last12w/last6m，空默认 last7d */
    private String range;
}
