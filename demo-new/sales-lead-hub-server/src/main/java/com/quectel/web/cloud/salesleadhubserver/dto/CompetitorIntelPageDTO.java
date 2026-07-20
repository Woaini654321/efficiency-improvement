package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 竞品情报分页入参。分页基础字段继承 {@link BasePageDTO}（注意基类无 keyword，本类自声明）。
 *
 * <p>注：前端情报中心是「全量拉取（pageSize=999）+ 客户端过滤」模式，实发只有
 * pageNumber/pageSize；brand/intelType/keyword 为契约预留，后端照常支持。
 * pageSize 上限由 service 层 {@code Math.min(500)} 夹逼（page 端点不加 @Valid，
 * 同 OpportunityController.page），999 → 截到 500，情报总量远小于 500 不受影响。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompetitorIntelPageDTO extends BasePageDTO {

    /** 关键词模糊搜索（标题/摘要），可空 */
    private String keyword;

    /** 竞品品牌，精确过滤 */
    private String brand;

    /** new_product/price_change/customer_case/other，精确过滤 */
    private String intelType;
}
