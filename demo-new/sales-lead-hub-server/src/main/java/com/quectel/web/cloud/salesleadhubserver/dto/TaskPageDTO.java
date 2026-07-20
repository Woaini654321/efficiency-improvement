package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 「我的任务」分页入参。分页四字段继承 {@link BasePageDTO}。
 *
 * <p>字段以前端 {@code TaskPageParams}（task/types.ts）为准。当前页面是全量拉取 +
 * 客户端 tab/排序，实发只有 pageNumber/pageSize；status/priority/sort 为契约预留，
 * 后端照常支持。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskPageDTO extends BasePageDTO {

    /** pending/processing/completed/transferred/cancelled */
    private String status;

    /** normal/urgent/critical */
    private String priority;

    /** 排序：deadline / priority（预留） */
    private String sort;
}
