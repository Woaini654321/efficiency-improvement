package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 商机需求模块业务错误码。
 *
 * <p>框架 {@code com.quectel.code.web.exception.ErrorCode} 只有
 * SUCCESS/BAD_REQUEST/PARAM_MISSING/PARAM_INVALID/UNAUTHORIZED/FORBIDDEN/NOT_FOUND/
 * INTERNAL_ERROR/SYSTEM_TIMEOUT/SYSTEM_BUSY，<b>无 CONFLICT 项</b>，故本模块自建。</p>
 *
 * <p>契约取证（javap quectel-code-web-starter）：
 * {@code IErrorCode { int getCode(); String getMessageKey(); }} ——
 * 是 <b>messageKey（i18n 键）</b>，不是字面文案。抛出时用
 * {@code BaseException(IErrorCode, String)} 重载传自定义文案，
 * 该重载会置 {@code isCustomMessage()=true}，从而跳过 MessageSource 解析。</p>
 *
 * <p>⚠️ code 刻意避开 401：前端 {@code @q-cli/libs} 拦截器的 {@code is401(code)}
 * 会触发重新登录流程，用 401 表达业务冲突会导致用户被莫名登出。</p>
 */
public enum RequirementErrorCode implements IErrorCode {

    /** 乐观锁版本冲突：加载后到提交前，该行已被他人更新。 */
    VERSION_CONFLICT(409, "requirement.version.conflict"),

    /** 非发布本人（且非管理员），无权修改。 */
    NOT_OWNER(403, "requirement.not.owner");

    private final int code;
    private final String messageKey;

    RequirementErrorCode(int code, String messageKey) {
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
