package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/** 分类模块业务错误码。code 避开 401（理由见 RequirementErrorCode）。 */
public enum CategoryErrorCode implements IErrorCode {

    /** 尚有子分类，不能删除。 */
    HAS_CHILDREN(409, "category.has.children"),

    /** 已被商机/需求引用，不能删除（先停用）。 */
    IN_USE(409, "category.in.use");

    private final int code;
    private final String messageKey;

    CategoryErrorCode(int code, String messageKey) {
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
