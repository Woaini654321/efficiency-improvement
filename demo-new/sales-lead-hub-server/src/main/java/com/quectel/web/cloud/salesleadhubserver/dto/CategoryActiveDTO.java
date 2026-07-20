package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/** 停用/启用入参，前端实发 {@code {id, isActive}}。 */
@Data
public class CategoryActiveDTO {

    @NotNull
    private Long id;

    @NotNull
    private Boolean isActive;
}
