package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 评论列表查询入参。对应前端 {@code getComments(targetType, targetId)} 的
 * body {@code { targetType, targetId }}。入参 camelCase，ID 用 String（前端 ID 全 string）。
 */
@Data
public class InteractionQueryDTO {

    /** 目标类型：Opportunity / Request / Response */
    @NotBlank
    private String targetType;

    /** 目标 id（数字雪花的字符串形式） */
    @NotBlank
    private String targetId;
}
