package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 商机模块业务错误码。code 避开 401（前端 is401 会触发重登），理由见 RequirementErrorCode。
 */
public enum OpportunityErrorCode implements IErrorCode {

    /** 乐观锁版本冲突：加载后到提交前，该行已被他人更新。 */
    VERSION_CONFLICT(409, "opportunity.version.conflict"),

    /** 非发布本人（且非管理员），无权修改。 */
    NOT_OWNER(403, "opportunity.not.owner"),

    /** 恢复上架只能由下架人本人或管理员执行（谁下架谁恢复）。 */
    NOT_ARCHIVER(403, "opportunity.not.archiver"),

    /** 状态流转非法（如对草稿执行下架）。 */
    ILLEGAL_TRANSITION(409, "opportunity.illegal.transition");

    private final int code;
    private final String messageKey;

    OpportunityErrorCode(int code, String messageKey) {
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
