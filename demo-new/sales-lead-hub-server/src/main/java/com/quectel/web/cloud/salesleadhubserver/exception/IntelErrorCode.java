package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 情报中心模块业务错误码。code 避开 401（前端 is401 会触发重登），理由见 RequirementErrorCode。
 */
public enum IntelErrorCode implements IErrorCode {

    /** 情报详情不存在（id 非法或已删除）。 */
    INTEL_NOT_FOUND(404, "intel.not.found");

    private final int code;
    private final String messageKey;

    IntelErrorCode(int code, String messageKey) {
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
