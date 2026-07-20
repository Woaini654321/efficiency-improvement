package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 分类创建入参，字段以前端实发 payload 为准（dictType 无列，不声明）。 */
@Data
public class CategoryCreateDTO {

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 128)
    private String nameEn;

    /** 父分类 id（string，Long→String 约定）；空=根节点 */
    private String parentId;

    private Integer sortOrder;

    @NotNull
    private Boolean isActive;
}
