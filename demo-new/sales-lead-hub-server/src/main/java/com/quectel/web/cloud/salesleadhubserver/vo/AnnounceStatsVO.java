package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 运营端公告统计卡出参（operation/announce/stats）。对齐前端
 * {@code getAnnounceStatsAdapter} 读取的 4 个 snake_case 键：total/published/draft/total_views。
 *
 * <p>字段刻意用 {@code Integer} 而非 {@code Long}：全局 Jackson 的 Long→String 只作用于 Long，
 * 若用 Long 会被序列化成字符串（如 {@code "total":"13"}），而 adapter 未对 stats 做 Number() 收敛，
 * 会造成 number 类型撒谎。用 Integer 直接序列化为 JSON 数字。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AnnounceStatsVO {

    /** 公告总数（全部状态） */
    private Integer total;

    /** 已发布数 */
    private Integer published;

    /** 草稿数 */
    private Integer draft;

    /** 累计浏览数（各公告 view_count 之和） */
    private Integer totalViews;
}
