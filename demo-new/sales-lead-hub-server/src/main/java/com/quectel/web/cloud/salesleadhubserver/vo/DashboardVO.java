package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营数据看板聚合出参。类级 @JsonNaming 出 snake_case；嵌套 VO 亦各自声明（策略不级联）。
 *
 * <p>计数用 Integer、比率/环比用 Double（不用 Long，避免全局 Long→String 把数字变字符串）。
 * 所有环比 mom = (cur-prev)/prev*100，prev=0 时取 0（不造假）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardVO {

    /** 独立访客（view_log 周期内去重 user） */
    private Integer uv;

    /** 页面浏览（view_log 周期内行数） */
    private Integer pv;

    private Double uvMom;

    private Double pvMom;

    /** 活跃用户（与 uv 同源同口径） */
    private Integer activeUsers;

    private Double activeUsersMom;

    /** 周期内新发布商机 + 需求数 */
    private Integer weekPublish;

    private Double weekPublishMom;

    /** 需求响应率（周期内 response_count>0 占比，%） */
    private Double responseRate;

    private Double responseRateMom;

    /** 需求采纳率（周期内 status=Adopted 占比，%） */
    private Double adoptRate;

    private Double adoptRateMom;

    private List<DashboardHotContentVO> hotContents;

    private List<DashboardCategoryDistVO> categoryDist;

    private List<DashboardPageHeatVO> pageHeat;

    private DashboardTrendMapVO weekPublishTrend;

    private DashboardTrendMapVO responseRateTrend;

    private List<DashboardPieSegVO> oppCategoryPie;

    private List<DashboardPieSegVO> demandCategoryPie;

    /** 近 24h 按小时分桶的活跃数，长度固定 24 */
    private List<Integer> hourlyActive;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
