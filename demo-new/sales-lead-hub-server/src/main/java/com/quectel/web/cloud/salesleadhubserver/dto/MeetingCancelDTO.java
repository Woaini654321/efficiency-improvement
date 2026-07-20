package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** 作废入参，前端实发 {@code {id, reason}}。 */
@Data
public class MeetingCancelDTO {

    @NotNull
    private Long id;

    /** 作废原因，可空 */
    @Size(max = 200)
    private String reason;
}
