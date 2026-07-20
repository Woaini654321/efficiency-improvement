package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** 仅携带 id 的通用入参（前端实发 {@code {id}}，如 delete 类端点）。 */
@Data
public class IdDTO {

    @NotNull
    private Long id;
}
