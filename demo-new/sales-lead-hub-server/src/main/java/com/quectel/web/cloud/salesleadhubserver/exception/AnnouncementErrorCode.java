package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 公告模块业务错误码。code 避开 401（前端 is401 会触发重登），风格对齐 OpportunityErrorCode。
 */
public enum AnnouncementErrorCode implements IErrorCode {

    /** 乐观锁版本冲突：加载后到提交前，该行已被他人更新。 */
    VERSION_CONFLICT(409, "announcement.version.conflict"),

    /** 状态流转非法（如对草稿执行下架、对已归档执行下架）。 */
    ILLEGAL_TRANSITION(409, "announcement.illegal.transition");

    private final int code;
    private final String messageKey;

    AnnouncementErrorCode(int code, String messageKey) {
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
