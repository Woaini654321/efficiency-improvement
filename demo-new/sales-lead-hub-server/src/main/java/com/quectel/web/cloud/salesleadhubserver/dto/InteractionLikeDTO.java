package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 点赞 / 收藏切换入参。对应前端 {@code likeComment(id)} 的 body {@code { id }}。
 *
 * <p><b>契约说明</b>：前端评论区的点赞按钮调用 {@code interaction/like} 时只传 {@code id}
 * （= 被点赞评论的 interaction_id），故 targetType 缺省视为 {@code Comment}、type 缺省 {@code like}。
 * 对内容主体（商机/需求）的点赞/收藏则显式传 targetType=Opportunity/Request 与 type=like/collect
 * （目标表带计数列，会做原子回写）。userId 不收，一律用当前登录人身份。</p>
 */
@Data
public class InteractionLikeDTO {

    /** 目标 id：前端点赞评论时=评论 interaction_id；点赞内容主体时=商机/需求 id */
    @NotBlank
    private String id;

    /** 目标类型，缺省 Comment；可为 Opportunity / Request / Response */
    private String targetType;

    /** 反应类型，缺省 like；可为 collect */
    private String type;
}
