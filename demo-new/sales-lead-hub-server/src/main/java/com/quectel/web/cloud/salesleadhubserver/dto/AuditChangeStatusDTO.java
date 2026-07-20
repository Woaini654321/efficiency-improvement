package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 运营强制上/下架入参，前端实发 {@code {id, status}}（audit/changeStatus）。
 *
 * <p>不带 contentType：目标表由 status 取值判定——published/archived 走 opportunity，
 * Closed 等走 opportunity_request；非法目标状态在 service 拒绝。</p>
 */
@Data
public class AuditChangeStatusDTO {

    @NotNull
    private Long id;

    /** 目标状态；取值合法性与目标表分流在 service 校验 */
    @NotBlank
    private String status;
}
