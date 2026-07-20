package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 运营置顶/取消置顶入参，前端实发 {@code {id, isPinned}}（audit/changePin）。
 *
 * <p>不带 contentType：目标表由 id（雪花全局唯一）先探 opportunity 再探
 * opportunity_request 判定。{@code sortNo} 前端当前不传，为契约预留——传了才一并写入。</p>
 */
@Data
public class AuditChangePinDTO {

    @NotNull
    private Long id;

    /** 置顶开关 */
    @NotNull
    private Boolean isPinned;

    /** 排序号，可空；前端当前不传，非空时一并写入 */
    private Integer sortNo;
}
