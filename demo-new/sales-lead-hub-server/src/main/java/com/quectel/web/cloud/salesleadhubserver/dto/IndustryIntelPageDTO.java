package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行业情报分页入参。分页基础字段继承 {@link BasePageDTO}（注意基类无 keyword，本类自声明）。
 *
 * <p>pageSize 上限由 service 层 {@code Math.min(500)} 夹逼（page 端点不加 @Valid），
 * 说明同 {@link CompetitorIntelPageDTO}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IndustryIntelPageDTO extends BasePageDTO {

    /** 关键词模糊搜索（标题/摘要），可空 */
    private String keyword;

    /** trend/automotive/policy/energy/industrial/smartcity，精确过滤 */
    private String industry;
}
