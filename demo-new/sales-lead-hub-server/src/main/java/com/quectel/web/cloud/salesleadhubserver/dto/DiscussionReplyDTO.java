package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 讨论回帖入参。对齐前端 detail 页回帖提交 payload：{@code {postId, parentId?, content}}。
 *
 * <p>{@code parentId} 可空：为空=对帖本身的一级回帖，非空=对某条回帖的追评。
 * service 层会校验 parentId 指向的回帖确属同一帖（防跨帖挂树）。</p>
 *
 * <p>{@code content} 是纯文本/短富文本，收窄到 1000 字（回帖非长文，与帖正文
 * MEDIUMTEXT 不同）。作者 id/name 不在入参，由 service 从本地 sys_user 快照，防伪造。</p>
 */
@Data
public class DiscussionReplyDTO {

    /** 所属讨论帖 id */
    @NotNull
    private Long postId;

    /** 父回帖 id，可空；非空时必须属于同一帖（不限层级） */
    private Long parentId;

    /** 回帖内容 */
    @NotBlank
    @Size(max = 1000)
    private String content;
}
