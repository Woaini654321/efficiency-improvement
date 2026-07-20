package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/** 商机更新入参 = 创建字段 + id + 乐观锁 version（两者缺一不可）。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpportunityUpdateDTO extends OpportunityCreateDTO {

    @NotNull
    private Long id;

    /** 客户端读取详情时拿到的 version，参与 WHERE version=? 冲突检测 */
    @NotNull
    private Integer version;
}
