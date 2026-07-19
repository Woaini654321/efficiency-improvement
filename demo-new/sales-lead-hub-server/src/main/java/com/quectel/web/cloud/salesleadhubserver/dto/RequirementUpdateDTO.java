package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 更新需求入参 = 新建入参 + id + version。
 *
 * <p>继承而非复制字段，避免两侧契约漂移；version 必带，乐观锁依赖它。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequirementUpdateDTO extends RequirementCreateDTO {

    @NotNull
    private Long id;

    @NotNull
    private Integer version;
}
