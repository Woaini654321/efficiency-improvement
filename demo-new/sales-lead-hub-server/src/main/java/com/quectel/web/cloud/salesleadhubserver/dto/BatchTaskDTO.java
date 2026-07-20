package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量发布中的单条任务。字段以前端 {@code BatchTaskParams}（batch/types.ts）为准。
 *
 * <p>{@code executorIds} 是 sys_user id（前端执行人选择器 value=id），发布时逐个
 * 校验存在且在职，落姓名快照。</p>
 */
@Data
public class BatchTaskDTO {

    @NotBlank
    @Size(max = 500)
    private String desc;

    /** normal / urgent / critical */
    @NotBlank
    private String priority;

    /** 截止时间，前端 valueFormat=YYYY-MM-DD */
    @NotBlank
    private String deadline;

    /** 执行人 sys_user id 集（至少 1 个） */
    @NotEmpty
    private List<String> executorIds;
}
