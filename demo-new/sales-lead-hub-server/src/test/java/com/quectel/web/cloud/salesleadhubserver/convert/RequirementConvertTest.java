package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementDetailVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * convert 映射 + 出参序列化契约的离线门禁。
 *
 * <p>第三条用例把"黑盒④ 序列化"的契约面拉进离线测试：不依赖 Spring 容器，
 * 纯 ObjectMapper 就能断言 snake_case 与日期格式，避免只能靠联调肉眼看。</p>
 */
class RequirementConvertTest {

    private final RequirementConvert convert = new RequirementConvert();

    @Test
    void toCreateDO_maps_business_fields_but_not_audit_or_publisher() {
        RequirementCreateDTO dto = new RequirementCreateDTO();
        dto.setTitle("5G 模组选型");
        dto.setDescription("desc");
        dto.setIndustry("IoT");
        dto.setUrgency("urgent");
        dto.setVisibilityType("all");

        OpportunityRequestDO d = convert.toCreateDO(dto);

        assertEquals("5G 模组选型", d.getTitle());
        assertEquals("all", d.getVisibilityScope(), "visibilityType -> DO.visibilityScope 桥接");
        assertNull(d.getPublisherId(), "publisherId 必须由 service 从 SSO 上下文回填");
        assertNull(d.getCreateBy(), "审计字段禁 convert 赋值，由框架 MetaObjectHandler 填充");
        assertNull(d.getId(), "id 由雪花算法生成");
    }

    @Test
    void toDetailVO_exposes_version_and_bridges_visibility() {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(123L);
        d.setStatus("Pending");
        d.setVersion(2);
        d.setVisibilityScope("dept");
        d.setCategoryNames(Arrays.asList("5G 模组"));

        RequirementDetailVO vo = convert.toDetailVO(d);

        assertEquals(Long.valueOf(123L), vo.getRequestId());
        assertEquals(Integer.valueOf(2), vo.getVersion(), "version 须下发，前端 update 要原样回传");
        assertEquals("dept", vo.getVisibilityType());
        assertEquals(Arrays.asList("5G 模组"), vo.getCategoryNames());
    }

    @Test
    void detailVO_serializes_to_snake_case_with_plain_datetime() throws Exception {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(1L);
        d.setVisibilityScope("all");
        d.setCategoryNames(Arrays.asList("5G 模组"));
        d.setCreateTime(LocalDateTime.of(2026, 7, 1, 9, 12, 0));

        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(convert.toDetailVO(d));

        assertTrue(json.contains("\"request_id\""), json);
        assertTrue(json.contains("\"category_names\""), json);
        assertTrue(json.contains("\"visibility_type\""), json);
        assertTrue(json.contains("\"2026-07-01 09:12:00\""),
                "日期须为空格分隔，ISO 的 T 分隔会让前端解析出 NaN: " + json);
        assertFalse(json.contains("\"requestId\""), "不得出现 camelCase: " + json);
    }
}
