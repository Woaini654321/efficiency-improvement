package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 需求分页入参。字段名随前端 {@code @q-mono-x/types/base} 的 PaginationParams
 * 及 requirement 模块扩展（keyword/urgency/status/sort，<b>有 sort、无 industry</b>）。
 *
 * <p>DTO 保持 camelCase：全局 Jackson 未开 snake_case 策略，前端实发即 camelCase。</p>
 */
@Data
public class RequirementPageDTO {

    @Min(1)
    private Integer pageNumber;

    /** 框架分页拦截器 maxLimit=500 会静默截断，此处显式夹逼避免"返 500 条但 total 是全量"。 */
    @Min(1)
    @Max(500)
    private Integer pageSize;

    private String orderBy;

    private String orderDirection;

    private String keyword;

    private String status;

    private String urgency;

    /** 前端扩展字段名是 sort（不是 industry）。 */
    private String sort;
}
