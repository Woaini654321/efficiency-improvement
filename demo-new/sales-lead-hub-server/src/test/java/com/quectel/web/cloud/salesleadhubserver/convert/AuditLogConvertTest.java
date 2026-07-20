package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 审计日志契约测试：快照 JSON 原样透传（对象而非转义字符串）+ IP 脱敏 + snake_case。 */
class AuditLogConvertTest {

    private final AuditLogConvert convert = new AuditLogConvert();

    @Test
    void vo_passes_snapshot_through_as_json_object_and_masks_ip() throws Exception {
        AuditLogDO d = new AuditLogDO();
        d.setId(3001L);
        d.setOperatorName("张伟");
        d.setActionType("archive");
        d.setTarget("方案：X");
        d.setResult("success");
        d.setIpAddress("10.12.33.101");
        d.setBeforeSnapshot("{\"status\":\"published\"}");
        d.setAfterSnapshot(null);
        d.setCreateTime(LocalDateTime.of(2026, 7, 19, 10, 0, 12));

        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(convert.toVO(d));

        assertTrue(json.contains("\"log_id\""), json);
        // @JsonRawValue：快照必须是对象，不能变成转义字符串 "{\"status\":...}"
        assertTrue(json.contains("\"before_snapshot\":{\"status\":\"published\"}"), json);
        assertFalse(json.contains("\\\"status\\\""), "快照被二次转义成字符串：" + json);
        // null 快照整键省略（NON_NULL），前端类型本就声明 | null
        assertFalse(json.contains("after_snapshot"), json);
        // IP 对外脱敏（schema §6）：保留前两段
        assertTrue(json.contains("\"ip_address\":\"10.12.*.*\""), json);
        assertTrue(json.contains("\"2026-07-19 10:00:12\""), json);
    }

    @Test
    void malformed_snapshot_degrades_to_null_not_broken_json() {
        AuditLogDO d = new AuditLogDO();
        d.setId(1L);
        d.setBeforeSnapshot("not-json{{{");
        // @JsonRawValue 会把非法串原样拼进响应、炸掉整个 JSON——convert 必须先校验
        assertEquals(null, convert.toVO(d).getBeforeSnapshot(),
                "非法 JSON 快照应降级为 null，绝不能原样透传");
    }
}
