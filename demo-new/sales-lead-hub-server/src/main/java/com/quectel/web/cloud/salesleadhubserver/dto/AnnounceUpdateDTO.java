package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 公告更新入参 = 创建字段 + id + 乐观锁 version。
 *
 * <p>与 opportunity 一致，客户端读取详情/列表拿到的 version 必须随 update 提交，
 * 参与 {@code WHERE version=?} 冲突检测（applyUpdate 里 setVersion）。前端 {@code AnnounceUpdateParams}
 * 目前尚未带 version —— 由 announce「前端切真」任务补 types.ts 的 version 字段（对齐配方第 12 步），
 * 本后端契约先按乐观锁标准形态定义。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnounceUpdateDTO extends AnnounceCreateDTO {

    @NotNull
    private Long id;

    /** 客户端读取时拿到的 version，参与 WHERE version=? 冲突检测 */
    @NotNull
    private Integer version;
}
