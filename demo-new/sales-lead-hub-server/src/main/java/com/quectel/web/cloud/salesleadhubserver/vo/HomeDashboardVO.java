package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 首页工作台聚合出参。类级 @JsonNaming 出 snake_case；各嵌套 VO 亦各自声明（策略不级联）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HomeDashboardVO {

    private HomeStatsVO stats;

    /** 热门标签（被内容引用最多的启用分类名） */
    private List<String> hotTags;

    /** 当前用户待办任务（pending/processing，deadline 正序，最多 5 条） */
    private List<HomeTaskVO> quickTasks;

    /** 热门方案 Top5 */
    private List<HomeSolutionVO> hotSolutions;

    /** 公告 Top3 */
    private List<HomeAnnouncementVO> announcements;

    /** 讨论热帖 */
    private List<HomePostVO> hotPosts;
}
