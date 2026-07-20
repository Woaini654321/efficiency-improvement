package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会议任务分页入参（管理端「会议任务」列表）。分页四字段继承 {@link BasePageDTO}。
 *
 * <p>字段以前端 {@code MeetingTaskPageParams}（meeting/types.ts）实发为准：
 * keyword 匹配会议名或任务描述；status/priority 为枚举过滤。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingPageDTO extends BasePageDTO {

    /** 关键词：匹配 meeting_name 或 task_desc */
    private String keyword;

    /** pending/processing/completed/transferred/cancelled */
    private String status;

    /** normal/urgent/critical */
    private String priority;
}
