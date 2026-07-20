package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 催办入参，前端实发 {@code {id, remark}}。 */
@Data
public class MeetingUrgeDTO {

    @NotNull
    private Long id;

    /** 催办备注，可空 */
    @Size(max = 200)
    private String remark;
}
