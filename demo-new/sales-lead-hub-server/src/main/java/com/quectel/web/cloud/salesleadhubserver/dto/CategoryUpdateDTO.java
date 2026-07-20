package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/** 分类更新入参。表无 version 列，无乐观锁（后写覆盖先写，运营单人维护可接受）。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryUpdateDTO extends CategoryCreateDTO {

    @NotNull
    private Long id;
}
