package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 发表评论 / 回复入参。对应前端 {@code addComment(params)} 的
 * body {@code { targetType, targetId, content, parentId? }}。
 *
 * <p>刻意<b>不收</b> userId：写操作一律用当前登录人身份（防伪造），
 * user_id / user_name 由 service 从本地 sys_user 取。</p>
 */
@Data
public class InteractionCommentDTO {

    /** 目标类型：Opportunity / Request / Response */
    @NotBlank
    private String targetType;

    /** 目标 id */
    @NotBlank
    private String targetId;

    /** 评论正文（service 内 trim 后再校验非空） */
    @NotBlank
    private String content;

    /** 父评论 id：为空=一级评论；非空须指向一条一级评论（≤2 级，PRD D7） */
    private String parentId;
}
