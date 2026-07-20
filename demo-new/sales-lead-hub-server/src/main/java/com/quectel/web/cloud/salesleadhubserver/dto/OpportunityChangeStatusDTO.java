package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 下架/恢复入参，前端实发 {@code {id, status}}。 */
@Data
public class OpportunityChangeStatusDTO {

    @NotNull
    private Long id;

    /** archived=下架 / published=恢复上架；取值合法性在 service 校验 */
    @NotBlank
    private String status;
}
