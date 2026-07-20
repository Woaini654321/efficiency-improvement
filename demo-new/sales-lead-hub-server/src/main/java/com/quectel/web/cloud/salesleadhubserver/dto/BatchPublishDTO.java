package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量发布入参。字段以前端 {@code BatchPublishParams}（batch/types.ts）与页面
 * {@code handlePublish()} 实发为准。
 *
 * <p>{@code meetingSource='exist'} 时用 {@code meetingId} 复用已有会议；{@code 'new'}
 * 时用 meetingName/meetingDate/recorderName 新建会议。整个发布同一事务。</p>
 */
@Data
public class BatchPublishDTO {

    /** exist / new */
    @NotBlank
    private String meetingSource;

    /** meetingSource=exist 时必填（前端 Long→String） */
    private Long meetingId;

    private String meetingName;

    /** 前端 valueFormat=YYYY-MM-DD */
    private String meetingDate;

    private String recorderName;

    @NotEmpty
    @Valid
    private List<BatchTaskDTO> tasks;
}
