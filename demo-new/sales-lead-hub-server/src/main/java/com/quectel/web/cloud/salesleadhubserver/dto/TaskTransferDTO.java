package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 转交任务入参，前端实发 {@code {id, transferTo, reason}}。
 *
 * <p>{@code transferTo} 取自前端转交选择器：当前实现 value=姓名（非 id）。后端兼容
 * 两种取值——纯数字按 sys_user id 解析取姓名，否则直接当姓名用（见 service）。</p>
 */
@Data
public class TaskTransferDTO {

    @NotNull
    private Long id;

    /** 转入对象（姓名或 sys_user id 字符串） */
    @NotNull
    @Size(max = 64)
    private String transferTo;

    /** 转交原因 */
    @Size(max = 200)
    private String reason;
}
