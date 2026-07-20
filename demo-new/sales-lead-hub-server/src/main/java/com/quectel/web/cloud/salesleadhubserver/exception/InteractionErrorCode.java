package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 互动模块业务错误码。code 避开 401（前端 is401 会触发重登），理由见 RequirementErrorCode。
 */
public enum InteractionErrorCode implements IErrorCode {

    /** 回复目标非法：parentId 未指向同目标下的一条一级评论（含三级回复、跨目标）。 */
    INVALID_PARENT_COMMENT(409, "interaction.invalid.parent"),

    /** 评论正文 trim 后为空。 */
    COMMENT_CONTENT_REQUIRED(400, "interaction.comment.required"),

    /** 评论正文超出长度上限（对齐前端 a-textarea maxlength=500）。 */
    COMMENT_CONTENT_TOO_LONG(400, "interaction.comment.too.long"),

    /** 不支持的目标类型（非 Opportunity/Request/Response/Comment）。 */
    UNSUPPORTED_TARGET_TYPE(400, "interaction.target.unsupported");

    private final int code;
    private final String messageKey;

    InteractionErrorCode(int code, String messageKey) {
        this.code = code;
        this.messageKey = messageKey;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessageKey() {
        return messageKey;
    }
}
