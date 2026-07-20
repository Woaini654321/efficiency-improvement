package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.quectel.web.cloud.salesleadhubserver.pojo.TransferRecord;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 「我的任务」列表项出参（task/page）。类级 @JsonNaming 出 snake_case。
 *
 * <p>出参键与前端 {@code TaskDTO}（task/types.ts）逐字对齐：在会议任务键基础上多
 * {@code transfer_from}（空串=非转交）与 {@code transfer_history}[{time,from,to,reason}]。</p>
 *
 * <p>{@link TransferRecord} 字段全小写单词（time/from/to/reason），snake 化是 no-op，
 * 无需在其上再标 @JsonNaming（类级策略不级联到嵌套对象，此处刚好无需级联）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TaskPageVO {

    /** 前端 adapter 读的键名是 task_id（不是 id） */
    private Long taskId;

    private String meetingName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime meetingDate;

    /** normal / urgent / critical */
    private String priority;

    /** pending / processing / completed / transferred / cancelled */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deadline;

    private String taskDesc;

    private String recorderName;

    /** 转出人姓名，空串=非转交（前端 adapter 用 ?? '' 兜底） */
    private String transferFrom;

    private List<String> assigneeNames;

    private List<TransferRecord> transferHistory;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
