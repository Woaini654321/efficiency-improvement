package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 讨论区模块业务错误码。code 避开 401（前端 is401 会触发重登），理由见 OpportunityErrorCode。
 */
public enum DiscussionErrorCode implements IErrorCode {

    /** topic 取值非法（不在 business/solution/experience/industry/complaint 内）。 */
    INVALID_TOPIC(400, "discussion.invalid.topic"),

    /** 回帖 parentId 指向的父回帖不存在，或不属于本帖（跨帖挂树）。 */
    INVALID_PARENT_REPLY(400, "discussion.invalid.parent.reply");

    private final int code;
    private final String messageKey;

    DiscussionErrorCode(int code, String messageKey) {
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
