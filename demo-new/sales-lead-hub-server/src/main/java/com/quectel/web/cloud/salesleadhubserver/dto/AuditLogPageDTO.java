package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 审计日志分页入参。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogPageDTO extends BasePageDTO {

    /** 匹配 target/操作人名 */
    private String keyword;

    private String actionType;

    /** success / failure */
    private String result;
}
