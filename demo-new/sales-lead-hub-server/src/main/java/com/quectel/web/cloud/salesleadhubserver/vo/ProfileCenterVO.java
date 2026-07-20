package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 个人中心聚合出参。类级 @JsonNaming 让 subscriptionTree→subscription_tree、
 * subscribedKeys→subscribed_keys（各嵌套 VO 亦各自标注 @JsonNaming）。
 *
 * <p><b>全段实装</b>：user/stats/subscription_tree(+subscribed_keys) 之外，补齐 adapter 实读的
 * 五段列表 collects/publishes/solutions/comments/view_history（页面 6 张统计卡与各 Tab 均自这些列表
 * 派生，是「数据全部来自服务端」的关键）。每段上限 100 行、按时间倒序，目标已逻辑删/物理删的行
 * （回查不到标题）一律丢弃。各嵌套 VO 亦各自标注 @JsonNaming。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileCenterVO {

    private ProfileUserVO user;

    private ProfileStatsVO stats;

    private SubscriptionTreeVO subscriptionTree;

    private SubscribedKeysVO subscribedKeys;

    private List<ProfileCollectVO> collects;

    private List<ProfilePublishVO> publishes;

    private List<ProfileSolutionVO> solutions;

    private List<ProfileCommentVO> comments;

    /** 前端读 view_history */
    private List<ProfileHistoryVO> viewHistory;
}
