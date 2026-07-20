package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 完成任务入参，前端实发 {@code {id, remark}}。 */
@Data
public class TaskCompleteDTO {

    @NotNull
    private Long id;

    /** 完成备注，可空 */
    @Size(max = 200)
    private String remark;
}
