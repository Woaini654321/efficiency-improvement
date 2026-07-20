package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 首页「我的任务」看板项（来自 meeting_task）。嵌套 VO 自带 @JsonNaming（类级策略不级联）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HomeTaskVO {

    /** 前端 adapter 读的键名是 task_id */
    private Long taskId;

    /** 任务描述（task_desc） */
    private String title;

    private String meetingName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deadline;

    /** normal/urgent/critical */
    private String priority;

    /** pending/processing/completed/transferred/cancelled（表值原样下发） */
    private String status;

    /** 是否已逾期（deadline < 服务器当前时间） */
    private Boolean isOverdue;
}
