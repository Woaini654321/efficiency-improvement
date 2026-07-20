package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 新增产品线成员入参。
 *
 * <p>入参 camelCase；id 一律 string（Long→String 约定），后端解析为 Long。
 * user_name 是快照，<b>不接受客户端传入</b>——由 service 从本地 sys_user 单行回填，
 * 防伪造。isOwner 为 1 表示 SLA L1 升级人，一条产品线至多一个，冲突由 service 校验。</p>
 */
@Data
public class MemberAddDTO {

    /** 产品线 id（string），必填 */
    @NotBlank
    private String productLineId;

    /** 用户 id（string），必填；须已开通且在职（sys_user.status='active'） */
    @NotBlank
    private String userId;

    /** 1=产品线负责人(L1 升级人)，0/空=普通成员 */
    private Integer isOwner;
}
