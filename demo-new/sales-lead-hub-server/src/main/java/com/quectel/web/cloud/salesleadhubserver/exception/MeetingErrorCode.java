package com.quectel.web.cloud.salesleadhubserver.exception;

import com.quectel.code.web.exception.IErrorCode;

/**
 * 会议任务模块业务错误码（meeting/task/batch 共用）。
 * code 避开 401（前端 is401 会触发重登），理由见 RequirementErrorCode。
 */
public enum MeetingErrorCode implements IErrorCode {

    /** 任务状态流转非法（如对已完成任务再开始、对已取消任务再完成）。 */
    ILLEGAL_TRANSITION(409, "meeting.illegal.transition"),

    /** 当前用户不是该任务执行人（且非管理员），无权操作。 */
    NOT_ASSIGNEE(403, "meeting.not.assignee"),

    /** 非任务创建人（且非管理员），无权管理该会议任务。 */
    NOT_OWNER(403, "meeting.not.owner"),

    /** 引用的会议不存在（批量发布 meetingSource=exist 时）。 */
    MEETING_NOT_FOUND(404, "meeting.not.found"),

    /** 批量发布指定的执行人不存在或已停用。 */
    EXECUTOR_INVALID(400, "meeting.executor.invalid");

    private final int code;
    private final String messageKey;

    MeetingErrorCode(int code, String messageKey) {
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
