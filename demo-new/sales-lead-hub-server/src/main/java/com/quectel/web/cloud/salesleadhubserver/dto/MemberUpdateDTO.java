package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 更新产品线成员入参。
 *
 * <p>刻意<b>不继承</b> {@link MemberAddDTO}：成员身份（productLineId+userId）是唯一键，
 * 不可原地改（换人=删旧行+加新行），本端点只切换 is_owner 负责人标记。product_line_member
 * 无 version 列（仅 create_time），故无乐观锁字段。设 owner 时的「唯一 owner」校验在 service 层。</p>
 */
@Data
public class MemberUpdateDTO {

    /** 成员行主键 id，必填 */
    @NotNull
    private Long id;

    /** 目标负责人标记：1=负责人，0=普通成员 */
    @NotNull
    private Integer isOwner;
}
