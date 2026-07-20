package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberAddDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberPageVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 产品线成员模块契约测试：DTO→DO 映射 + VO 离线序列化门禁。
 *
 * <p>序列化断言不必等联调：纯 ObjectMapper 就能钉死 snake_case 键名、日期格式与
 * 「不出现 camelCase」。</p>
 */
class MemberConvertTest {

    private final MemberConvert convert = new MemberConvert();

    @Test
    void toAddDO_maps_identity_and_leaves_userName_alone() {
        MemberAddDTO dto = new MemberAddDTO();
        dto.setProductLineId("501");
        dto.setUserId("9002");
        dto.setIsOwner(1);

        ProductLineMemberDO d = convert.toAddDO(dto, 501L, 9002L);

        assertEquals(Long.valueOf(501L), d.getProductLineId());
        assertEquals(Long.valueOf(9002L), d.getUserId());
        assertEquals(Integer.valueOf(1), d.getIsOwner());
        // user_name 快照由 service 从本地 sys_user 回填，convert 不碰（防客户端伪造）
        assertNull(d.getUserName());
    }

    @Test
    void toAddDO_defaults_isOwner_to_zero_when_null() {
        MemberAddDTO dto = new MemberAddDTO();
        dto.setProductLineId("501");
        dto.setUserId("9002");
        // isOwner 不传

        ProductLineMemberDO d = convert.toAddDO(dto, 501L, 9002L);
        assertEquals(Integer.valueOf(0), d.getIsOwner());
    }

    @Test
    void pageVO_serializes_snake_case_and_safe_date() throws Exception {
        ProductLineMemberDO d = new ProductLineMemberDO();
        d.setId(601L);
        d.setProductLineId(501L);
        d.setUserId(9003L);
        d.setUserName("李娜");
        d.setIsOwner(1);
        d.setCreateTime(LocalDateTime.of(2026, 7, 1, 9, 12, 0));

        MemberPageVO vo = convert.toPageVO(d);
        vo.setProductLineName("无线模组产品线");
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        assertTrue(json.contains("\"member_id\""), json);
        assertTrue(json.contains("\"product_line_id\""), json);
        assertTrue(json.contains("\"product_line_name\""), json);
        assertTrue(json.contains("\"user_id\""), json);
        assertTrue(json.contains("\"user_name\""), json);
        assertTrue(json.contains("\"is_owner\""), json);
        assertFalse(json.contains("productLineName"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("userName"), "不得出现 camelCase：" + json);
        // 默认 ISO 的 'T' 分隔会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
        assertTrue(json.contains("\"2026-07-01 09:12:00\""), json);
        assertFalse(json.contains("2026-07-01T09:12"), "禁 ISO 'T' 分隔：" + json);
    }

    @Test
    void detailVO_serializes_snake_case_and_safe_date() throws Exception {
        ProductLineMemberDO d = new ProductLineMemberDO();
        d.setId(601L);
        d.setProductLineId(501L);
        d.setUserId(9003L);
        d.setUserName("李娜");
        d.setIsOwner(0);
        d.setCreateTime(LocalDateTime.of(2026, 7, 1, 9, 12, 0));

        MemberDetailVO vo = convert.toDetailVO(d);
        vo.setProductLineName("无线模组产品线");
        vo.setEmployeeId("E1002");
        vo.setDepartmentName("华东大区");
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        assertTrue(json.contains("\"member_id\""), json);
        assertTrue(json.contains("\"employee_id\""), json);
        assertTrue(json.contains("\"department_name\""), json);
        assertFalse(json.contains("employeeId"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("departmentName"), "不得出现 camelCase：" + json);
        assertTrue(json.contains("\"2026-07-01 09:12:00\""), json);
        assertFalse(json.contains("2026-07-01T09:12"), "禁 ISO 'T' 分隔：" + json);
    }
}
