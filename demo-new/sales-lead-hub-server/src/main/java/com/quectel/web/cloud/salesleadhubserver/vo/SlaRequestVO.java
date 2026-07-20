package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求时效监控列表项出参。类级 @JsonNaming 出 snake_case（禁全局策略，理由见 OpportunityPageVO）。
 *
 * <p>sla_status/escalation_level/remaining_text/deadline 均为<b>查询时按服务器时间实时派生</b>，
 * 不取库里存量值（存量仅作参考）；deadline = create_time + 首响阈值。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SlaRequestVO {

    /** 前端 adapter 读的键名是 request_id */
    private Long requestId;

    private String title;

    /** normal/urgent/critical */
    private String urgency;

    /** normal/warning/overdue/responded（实时派生） */
    private String slaStatus;

    /** SLA 首响计时起点 = 需求 create_time */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** create_time + 首响阈值（critical 2h / urgent 4h / normal 24h） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deadline;

    /** 剩余/超时展示文案，如「剩余 1时30分」/「已超时 3时0分」/「已响应」（后端按服务器时间算） */
    private String remainingText;

    private Integer responseCount;

    /** L0/L1/L2/L3（实时派生） */
    private String escalationLevel;

    private String publisherName;

    private List<SlaTimelineVO> escalationTimeline;
}
