package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 方案响应/采纳/关闭模块业务错误码。
 *
 * <p>与 {@link RequirementErrorCode} 分开：本枚举承载「响应闭环」独有的状态冲突码；
 * 归属校验（NOT_OWNER）与乐观锁冲突（VERSION_CONFLICT）复用 RequirementErrorCode，
 * 不重复定义。</p>
 *
 * <p>code 刻意避开 401（前端拦截器 is401 会触发重登），冲突类统一用 409。</p>
 */
public enum ResponseErrorCode implements IErrorCode {

    /** 需求已采纳过方案：采纳单一真相源，不允许二次采纳。 */
    REQUIREMENT_ALREADY_ADOPTED(409, "requirement.already.adopted"),

    /** 需求已关闭：关闭后不再接受提交/采纳。 */
    REQUIREMENT_CLOSED(409, "requirement.closed"),

    /** 方案不属于该需求：responseId 与 requestId 不匹配。 */
    RESPONSE_NOT_MATCH(400, "response.not.match"),

    /** 非法关闭态：仅 Pending/Collecting 可关闭。 */
    ILLEGAL_CLOSE_STATE(409, "requirement.illegal.close");

    private final int code;
    private final String messageKey;

    ResponseErrorCode(int code, String messageKey) {
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
