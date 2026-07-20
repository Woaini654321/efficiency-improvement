package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.Attachment;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityDetailVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商机模块契约测试：DTO→DO 映射 + VO 离线序列化门禁。
 *
 * <p>序列化断言不必等联调：纯 ObjectMapper 就能钉死 snake_case 键名、
 * 日期格式与「不出现 camelCase」。</p>
 */
class OpportunityConvertTest {

    private final OpportunityConvert convert = new OpportunityConvert();

    @Test
    void toCreateDO_maps_payload_fields_and_leaves_publisher_alone() {
        OpportunityCreateDTO dto = new OpportunityCreateDTO();
        dto.setTitle("5G RedCap 选型方案");
        dto.setType("solution");
        dto.setStatus("draft");
        dto.setSummary("摘要");
        dto.setContent("<p>正文</p>");
        dto.setCategoryIds(Arrays.asList("101", "10101"));

        OpportunityDO d = convert.toCreateDO(dto);

        assertEquals("5G RedCap 选型方案", d.getTitle());
        assertEquals("solution", d.getType());
        assertEquals("draft", d.getStatus());
        assertEquals("摘要", d.getSummary());
        assertEquals("<p>正文</p>", d.getContent());
        // publisher* 由 service 从 SSO 上下文回填，convert 不碰（防客户端伪造）
        assertNull(d.getPublisherId());
        assertNull(d.getPublisherName());
    }

    @Test
    void applyUpdate_carries_client_version_into_where_clause() {
        OpportunityUpdateDTO dto = new OpportunityUpdateDTO();
        dto.setId(1L);
        dto.setVersion(7);
        dto.setTitle("新标题");
        dto.setType("product_info");
        dto.setStatus("published");

        OpportunityDO existing = new OpportunityDO();
        existing.setVersion(9);   // 库里已是 9，客户端还拿着 7 的旧快照

        convert.applyUpdate(dto, existing);

        // 乐观锁靠实体 version 参与 WHERE version=?：必须回填客户端的 7，
        // 沿用库里的 9 会让任何陈旧提交都被当成最新提交放行
        assertEquals(Integer.valueOf(7), existing.getVersion());
        assertEquals("新标题", existing.getTitle());
    }

    @Test
    void detailVO_serializes_snake_case_safe_date_and_attachments() throws Exception {
        OpportunityDO d = new OpportunityDO();
        d.setId(123456789012345678L);
        d.setTitle("5G RedCap 选型方案");
        d.setPublisherDeptName("上海销售组");
        d.setCategoryNames(Arrays.asList("5G 模组"));
        Attachment a = new Attachment();
        a.setName("规格书.pdf");
        a.setUrl("https://oss/spec.pdf");
        a.setSize(2048L);
        d.setAttachments(Collections.singletonList(a));
        d.setCreateTime(LocalDateTime.of(2026, 7, 1, 9, 12, 0));
        d.setVersion(3);

        OpportunityDetailVO vo = convert.toDetailVO(d);
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        // 前端 adapter 读的是 opportunity_id，不是 id
        assertTrue(json.contains("\"opportunity_id\""), json);
        assertTrue(json.contains("\"publisher_dept_name\""), json);
        assertTrue(json.contains("\"category_names\""), json);
        assertFalse(json.contains("publisherDeptName"), "不得出现 camelCase：" + json);
        // 默认 ISO 的 'T' 分隔会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
        assertTrue(json.contains("\"2026-07-01 09:12:00\""), json);
        assertFalse(json.contains("2026-07-01T09:12"), "禁 ISO 'T' 分隔：" + json);
        // 附件按 {name,url,size} 原样下发
        assertTrue(json.contains("\"规格书.pdf\""), json);
        assertTrue(json.contains("\"version\":3"), json);
    }
}
