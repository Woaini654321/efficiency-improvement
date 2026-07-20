package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议任务列表项出参（meeting/page）。类级 @JsonNaming 出 snake_case（禁全局策略）。
 *
 * <p>出参键与前端 {@code MeetingTaskDTO}（meeting/types.ts）逐字对齐：
 * task_id/meeting_name/meeting_date/recorder_name/task_desc/priority/deadline/
 * assignee_names/status/created_at。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MeetingTaskPageVO {

    /** 前端 adapter 读的键名是 task_id（不是 id） */
    private Long taskId;

    private String meetingName;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime meetingDate;

    private String recorderName;

    private String taskDesc;

    /** normal / urgent / critical */
    private String priority;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deadline;

    private List<String> assigneeNames;

    /** pending / processing / completed / transferred / cancelled */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
