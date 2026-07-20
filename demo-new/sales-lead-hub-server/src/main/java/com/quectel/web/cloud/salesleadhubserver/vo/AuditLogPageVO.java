package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志列表项出参。
 *
 * <p>快照列 DB 存 JSON 文本，契约要求对象——{@code @JsonRawValue} 原样拼接；
 * 前提是 convert 已做合法性校验（非法串会炸掉整个响应 JSON，见 AuditLogConvert）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuditLogPageVO {

    /** 前端 adapter 读的键名是 log_id */
    private Long logId;

    private String operatorName;

    /** publish/archive/delete/role_change/isolation_change/category_change/login/sla_escalation */
    private String actionType;

    private String target;

    /** success / failure */
    private String result;

    /** 已脱敏（保留前两段），schema §6 要求对外不露全 IP */
    private String ipAddress;

    private String userAgent;

    @JsonRawValue
    private String beforeSnapshot;

    @JsonRawValue
    private String afterSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
