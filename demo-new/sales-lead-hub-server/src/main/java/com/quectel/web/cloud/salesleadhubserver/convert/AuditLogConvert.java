package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditLogPageVO;
import org.springframework.stereotype.Component;

/** 审计日志 DO → VO：快照合法性校验 + IP 脱敏的唯一桥接点。 */
@Component
public class AuditLogConvert {

    private static final ObjectMapper VALIDATOR = new ObjectMapper();

    public AuditLogPageVO toVO(AuditLogDO d) {
        AuditLogPageVO v = new AuditLogPageVO();
        v.setLogId(d.getId());
        v.setOperatorName(d.getOperatorName());
        v.setActionType(d.getActionType());
        v.setTarget(d.getTarget());
        v.setResult(d.getResult());
        v.setIpAddress(maskIp(d.getIpAddress()));
        v.setUserAgent(d.getUserAgent());
        v.setBeforeSnapshot(safeJson(d.getBeforeSnapshot()));
        v.setAfterSnapshot(safeJson(d.getAfterSnapshot()));
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    /**
     * {@code @JsonRawValue} 会把字段值<b>原样拼进</b>响应——若列里存了非法 JSON，
     * 整个响应体都会变成非法 JSON（不是这一个字段坏，是整页接口坏）。
     * 故透传前必须校验，非法降级为 null（NON_NULL 下整键省略，契约本就允许 null）。
     */
    private String safeJson(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            VALIDATOR.readTree(raw);
            return raw;
        } catch (Exception e) {
            return null;
        }
    }

    /** IP 对外脱敏（schema §6）：保留前两段，如 10.12.*.*；IPv6/异常格式整体打码。 */
    private String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".*.*";
        }
        return "***";
    }
}
