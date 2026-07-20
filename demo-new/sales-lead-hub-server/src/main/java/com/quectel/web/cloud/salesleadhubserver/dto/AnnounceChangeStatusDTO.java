package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 公告状态变更入参，前端实发 {@code {id, status}}（发布/下架/重新发布）。 */
@Data
public class AnnounceChangeStatusDTO {

    @NotNull
    private Long id;

    /** 目标状态 published/archived；取值合法性与状态机在 service 校验 */
    @NotBlank
    private String status;
}
