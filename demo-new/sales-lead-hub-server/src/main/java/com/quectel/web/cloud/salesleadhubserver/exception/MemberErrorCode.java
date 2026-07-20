package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 产品线成员模块业务错误码。code 避开 401（前端 is401 会触发重登，理由见 OpportunityErrorCode）。
 */
public enum MemberErrorCode implements IErrorCode {

    /** 该产品线已有其他负责人：一条产品线至多一个 owner（SLA L1 升级人唯一）。 */
    OWNER_CONFLICT(409, "member.owner.conflict"),

    /** 同一产品线重复添加同一用户（命中唯一键 uk_plm_line_user）。 */
    DUPLICATE_MEMBER(409, "member.duplicate");

    private final int code;
    private final String messageKey;

    MemberErrorCode(int code, String messageKey) {
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
