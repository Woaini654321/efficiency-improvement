package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 首页工作台顶部 4 统计卡。计数用 Integer（不用 Long，避免全局 Long→String 把数字变字符串）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HomeStatsVO {

    /** 已发布商机数 */
    private Integer solutionTotal;

    /** 待响应需求数（status=Pending） */
    private Integer pendingRequests;

    /** 近 7 天讨论帖数 */
    private Integer weekDiscussions;

    /** 近 7 天活跃用户数（view_log 去重） */
    private Integer activeUsers;
}
