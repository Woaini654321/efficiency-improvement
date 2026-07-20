package com.quectel.web.cloud.salesleadhubserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.ProductLineOptionServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.vo.ProductLineOptionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品线选项 service 测试：只取启用列表 + 映射为选项 VO；外加 VO 离线 snake_case 序列化门禁。
 * dao 全 mock，离线可跑。
 */
class ProductLineOptionServiceImplTest {

    @Mock ProductLineDao productLineDao;

    private ProductLineOptionServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new ProductLineOptionServiceImpl(productLineDao);
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private ProductLineDO pl(Long id, String name) {
        ProductLineDO d = new ProductLineDO();
        d.setId(id);
        d.setName(name);
        return d;
    }

    @Test
    void list_uses_active_only_dao_and_maps_to_option_vo_in_order() {
        // service 只调 is_active=1 的专用查询（过滤 is_active 在 dao 的 WHERE 内）
        when(productLineDao.listActiveOrderByName())
                .thenReturn(Arrays.asList(pl(501L, "低功耗产品线"), pl(502L, "无线模组产品线")));

        List<ProductLineOptionVO> vos = service.list();

        // 断言 service 走的是 active-only 查询，而非 list()/全表
        verify(productLineDao).listActiveOrderByName();
        assertEquals(2, vos.size());
        // 保持 dao 返回顺序（dao 已按 name 升序）
        assertEquals(Long.valueOf(501L), vos.get(0).getProductLineId());
        assertEquals("低功耗产品线", vos.get(0).getName());
        assertEquals(Long.valueOf(502L), vos.get(1).getProductLineId());
    }

    @Test
    void vo_serializes_snake_case_and_no_camel_case() throws Exception {
        ProductLineOptionVO v = new ProductLineOptionVO();
        v.setProductLineId(123456789012345678L);
        v.setName("无线模组产品线");

        String json = new ObjectMapper().writeValueAsString(v);

        assertTrue(json.contains("\"product_line_id\""), json);
        assertTrue(json.contains("\"name\""), json);
        assertFalse(json.contains("productLineId"), "不得出现 camelCase：" + json);
    }
}
