package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商机分页入参。分页四字段继承 {@link BasePageDTO}。
 *
 * <p>注：当前前端列表页是「全量拉取 + 客户端过滤/排序」模式，实发只有
 * pageNumber/pageSize；keyword/type/status 为契约预留（OpportunityPageParams
 * 已声明），后端照常支持，前端何时下沉过滤不影响本契约。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpportunityPageDTO extends BasePageDTO {

    private String keyword;

    /** product_info / solution / success_case */
    private String type;

    /** draft / published / archived */
    private String status;

    /** 前端契约字段名是 sort */
    private String sort;
}
