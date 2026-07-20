package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditPageVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运营审核契约测试：两种 content_type 的行离线序列化门禁。
 *
 * <p>纯 ObjectMapper 就能钉死 snake_case 键名、日期格式与 content_type 取值，不必等联调。</p>
 */
class AuditConvertTest {

    private final AuditConvert convert = new AuditConvert();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void opportunity_row_serializes_snake_case_and_content_type_opportunity() throws Exception {
        OpportunityDO d = new OpportunityDO();
        d.setId(123456789012345678L);
        d.setTitle("5G RedCap 选型方案");
        d.setPublisherName("张伟");
        d.setStatus("published");
        d.setIsPinned(true);
        d.setSortNo(3);
        d.setCreateTime(LocalDateTime.of(2026, 7, 1, 9, 12, 0));

        AuditPageVO vo = convert.toPageVO(d);
        assertEquals("opportunity", vo.getContentType());
        // 商机表无 urgency 列，统一落 normal
        assertEquals("normal", vo.getUrgency());

        String json = mapper.writeValueAsString(vo);
        // 前端 adapter 读的键名
        assertTrue(json.contains("\"audit_id\""), json);
        assertTrue(json.contains("\"content_type\":\"opportunity\""), json);
        assertTrue(json.contains("\"publisher_name\""), json);
        assertTrue(json.contains("\"is_pinned\":true"), json);
        assertTrue(json.contains("\"sort_no\":3"), json);
        // 不得出现 camelCase
        assertFalse(json.contains("auditId"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("contentType"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("publisherName"), "不得出现 camelCase：" + json);
        // 默认 ISO 'T' 分隔会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
        assertTrue(json.contains("\"published_at\":\"2026-07-01 09:12:00\""), json);
        assertFalse(json.contains("2026-07-01T09:12"), "禁 ISO 'T' 分隔：" + json);
    }

    @Test
    void request_row_serializes_snake_case_and_carries_urgency_industry() throws Exception {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(987654321098765432L);
        d.setTitle("求 NB-IoT 智慧水表低功耗方案");
        d.setPublisherName("李娜");
        d.setStatus("Collecting");
        d.setUrgency("urgent");
        d.setIndustry("智慧城市");
        d.setIsPinned(false);
        d.setSortNo(2);
        d.setCreateTime(LocalDateTime.of(2026, 6, 19, 9, 30, 0));

        AuditPageVO vo = convert.toPageVO(d);
        assertEquals("request", vo.getContentType());
        assertEquals("urgent", vo.getUrgency());
        assertEquals("智慧城市", vo.getIndustry());

        String json = mapper.writeValueAsString(vo);
        assertTrue(json.contains("\"content_type\":\"request\""), json);
        assertTrue(json.contains("\"urgency\":\"urgent\""), json);
        assertTrue(json.contains("\"industry\":\"智慧城市\""), json);
        assertTrue(json.contains("\"is_pinned\":false"), json);
        assertTrue(json.contains("\"published_at\":\"2026-06-19 09:30:00\""), json);
        assertFalse(json.contains("contentType"), "不得出现 camelCase：" + json);
    }
}
