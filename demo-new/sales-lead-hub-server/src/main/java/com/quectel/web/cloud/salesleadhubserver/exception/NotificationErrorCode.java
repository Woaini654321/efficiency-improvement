package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 通知模块业务错误码。code 避开 401（前端 is401 会触发重登），理由见 OpportunityErrorCode。
 */
public enum NotificationErrorCode implements IErrorCode {

    /** 该通知不属于当前登录人，无权标记已读。 */
    NOT_OWNER(403, "notification.not.owner");

    private final int code;
    private final String messageKey;

    NotificationErrorCode(int code, String messageKey) {
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
