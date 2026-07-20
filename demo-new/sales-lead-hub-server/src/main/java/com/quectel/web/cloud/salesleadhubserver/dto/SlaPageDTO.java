package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求时效监控分页入参。字段以前端 {@code apis/sla/slaApi.ts} 实发 payload 为准：
 * pageNumber/pageSize（继承自 {@link BasePageDTO}）+ urgency/slaStatus/startDate/endDate 四个可选筛选。
 *
 * <p>入参保持 camelCase（全局未开 snake_case，理由见 {@link BasePageDTO}）。
 * 前端列表页实际以 {@code pageSize:999} 拉全量后在本地过滤/分页，故 controller 不加
 * {@code @Valid}（否则 999 > @Max(500) 会 400），service 再夹逼到 maxLimit。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SlaPageDTO extends BasePageDTO {

    /** normal/urgent/critical，可空 */
    private String urgency;

    /** normal/warning/overdue/responded，可空（按实时派生状态过滤） */
    private String slaStatus;

    /** 起始日期 yyyy-MM-dd，按 create_time 过滤，可空 */
    private String startDate;

    /** 截止日期 yyyy-MM-dd，按 create_time 过滤，可空 */
    private String endDate;
}
