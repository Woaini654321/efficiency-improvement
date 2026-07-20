package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 会议任务创建入参。字段以前端 {@code MeetingTaskSaveParams}（meeting/types.ts）
 * 与页面 {@code handleSave()} 实发 payload 为准。
 *
 * <p>{@code assigneeNames} 只传姓名（前端选择器 value=姓名，非 id）；后端据此落
 * {@code assignee_names} 快照，{@code assignee_ids} 在过渡态留空，任务视图按姓名匹配。</p>
 */
@Data
public class MeetingCreateDTO {

    @NotBlank
    @Size(max = 200)
    private String meetingName;

    /** 会议时间，前端 DatePicker valueFormat=YYYY-MM-DD（也兼容含时分秒） */
    @NotBlank
    private String meetingDate;

    @Size(max = 64)
    private String recorderName;

    @NotBlank
    @Size(max = 500)
    private String taskDesc;

    /** normal / urgent / critical */
    @NotBlank
    private String priority;

    /** 截止时间，前端 valueFormat=YYYY-MM-DD HH:mm:ss */
    @NotBlank
    private String deadline;

    /** 执行人姓名集（至少 1 个） */
    @NotEmpty
    private List<String> assigneeNames;
}
