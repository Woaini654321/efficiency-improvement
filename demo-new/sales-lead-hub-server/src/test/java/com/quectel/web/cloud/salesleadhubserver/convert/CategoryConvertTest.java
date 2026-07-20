package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 分类模块契约测试：Boolean↔Integer 转换 + snake_case + NON_NULL 下 parent_id 缺键安全。 */
class CategoryConvertTest {

    private final CategoryConvert convert = new CategoryConvert();

    @Test
    void toCreateDO_converts_boolean_active_to_int() {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("5G 模组");
        dto.setNameEn("5G Module");
        dto.setParentId("1");
        dto.setSortOrder(2);
        dto.setIsActive(Boolean.TRUE);

        CategoryDO d = convert.toCreateDO(dto);

        assertEquals("5G 模组", d.getName());
        assertEquals(Long.valueOf(1L), d.getParentId());
        assertEquals(Integer.valueOf(1), d.getIsActive(), "DB 列 TINYINT(1)，true→1");
    }

    @Test
    void vo_serializes_snake_case_bool_and_omits_null_parent() throws Exception {
        CategoryDO root = new CategoryDO();
        root.setId(1L);
        root.setName("IoT 模组");
        root.setNameEn("IoT Module");
        root.setParentId(null);          // 根节点
        root.setSortOrder(1);
        root.setIsActive(1);

        ObjectMapper snakeAware = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);   // 框架全局 NON_NULL
        String json = snakeAware.writeValueAsString(convert.toVO(root, 128));

        assertTrue(json.contains("\"category_id\""), json);
        assertTrue(json.contains("\"name_en\""), json);
        assertTrue(json.contains("\"is_active\":true"), "VO 输出必须是 boolean 而非 0/1：" + json);
        assertTrue(json.contains("\"content_count\":128"), json);
        // NON_NULL：根节点 parent_id 整个键消失，前端 adapter 的 ?? '' 兜底
        assertFalse(json.contains("parent_id"), "null parent_id 应整键省略：" + json);
        assertFalse(json.contains("nameEn"), "不得出现 camelCase：" + json);
    }
}
