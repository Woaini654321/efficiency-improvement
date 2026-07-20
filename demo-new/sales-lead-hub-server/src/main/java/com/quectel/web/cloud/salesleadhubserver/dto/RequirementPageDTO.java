package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求分页入参。分页四字段继承 {@link BasePageDTO}；
 * 模块扩展字段 keyword/status/urgency/sort（<b>有 sort、无 industry</b>，随前端实发）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequirementPageDTO extends BasePageDTO {

    private String keyword;

    private String status;

    private String urgency;

    /** 前端扩展字段名是 sort（不是 industry）。 */
    private String sort;
}
