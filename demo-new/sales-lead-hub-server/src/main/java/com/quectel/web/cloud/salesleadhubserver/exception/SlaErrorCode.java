package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * SLA 时效监控模块业务错误码。code 避开 401（前端 is401 会触发重登，理由见 RequirementErrorCode）。
 */
public enum SlaErrorCode implements IErrorCode {

    /** 催办目标需求不存在。 */
    REQUEST_NOT_FOUND(404, "sla.request.not.found"),

    /** 催办目标类别解析不出任何本地用户（无发布人/部门负责人/产品线负责人）。 */
    NO_VALID_TARGET(400, "sla.urge.no.valid.target");

    private final int code;
    private final String messageKey;

    SlaErrorCode(int code, String messageKey) {
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
