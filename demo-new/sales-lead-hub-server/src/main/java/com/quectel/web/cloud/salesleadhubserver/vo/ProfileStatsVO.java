package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 个人中心-统计段，6 个计数（键名对齐前端 adapter）。
 *
 * <p>计数口径（全部限定「当前登录人」）：
 * collect_count = interaction(type=collect)；
 * comment_count = interaction(type=comment 且未软删)；
 * publish_count = 我发布的已发布商机 + 我发布的需求；
 * solution_count = 我提交的方案响应；
 * draft_count = 我的草稿商机；
 * view_count = 我的浏览记录（view_log）条数。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileStatsVO {

    private Integer collectCount;

    private Integer commentCount;

    private Integer publishCount;

    private Integer solutionCount;

    private Integer draftCount;

    private Integer viewCount;
}
