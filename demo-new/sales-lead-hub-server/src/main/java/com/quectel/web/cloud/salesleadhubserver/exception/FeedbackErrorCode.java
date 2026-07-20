package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 吐槽墙模块业务错误码。code 避开 401（前端 is401 会触发重登），理由见 RequirementErrorCode。
 */
public enum FeedbackErrorCode implements IErrorCode {

    /** 点赞目标不存在（已被清理或 id 非法）。 */
    FEEDBACK_NOT_FOUND(404, "feedback.not.found");

    private final int code;
    private final String messageKey;

    FeedbackErrorCode(int code, String messageKey) {
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
