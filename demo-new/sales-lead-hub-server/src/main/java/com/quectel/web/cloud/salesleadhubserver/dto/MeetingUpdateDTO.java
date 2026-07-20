package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 会议任务更新入参 = 创建字段 + 任务 id。
 *
 * <p>meeting_task 表无乐观锁列（schema 未定义 version），故不带 version 字段——
 * 与配方中「有 version 的模块」区别对待。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingUpdateDTO extends MeetingCreateDTO {

    /** 任务行 id（前端 Long→String，反序列化回 Long） */
    @NotNull
    private Long id;
}
