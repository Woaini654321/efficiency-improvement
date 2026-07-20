package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardTrendMapVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运营看板契约测试：VO 序列化——snake_case 键名（含嵌套/趋势）、安全日期格式、无 camelCase。
 */
class DashboardConvertTest {

    private final DashboardConvert convert = new DashboardConvert();

    @Test
    void dashboardVO_serializes_snake_case_nested_and_safe_date() throws Exception {
        OpportunityDO d = new OpportunityDO();
        d.setId(1003L);
        d.setTitle("车载 AG59x");
        d.setType("product_info");
        d.setViewCount(1560);

        DashboardVO vo = new DashboardVO();
        vo.setUv(120);
        vo.setUvMom(12.5);
        vo.setWeekPublish(8);
        vo.setResponseRate(82.5);
        vo.setHotContents(Collections.singletonList(convert.toHotContentVO(d)));
        vo.setCategoryDist(Collections.singletonList(convert.toCategoryDistVO("5G 模组", 10, 40.0)));
        vo.setPageHeat(Collections.singletonList(convert.toPageHeatVO("商机详情", 30, 60.0)));
        vo.setOppCategoryPie(Collections.singletonList(convert.toPieSegVO("5G 模组", 10, "#1677ff")));
        vo.setDemandCategoryPie(Collections.singletonList(convert.toPieSegVO("NB-IoT", 5, "#52c41a")));
        vo.setHourlyActive(Arrays.asList(1, 2, 3));
        DashboardTrendMapVO trend = new DashboardTrendMapVO();
        trend.setLast7d(Arrays.asList(1, 2, 3));
        vo.setWeekPublishTrend(trend);
        vo.setResponseRateTrend(trend);
        vo.setUpdatedAt(LocalDateTime.of(2026, 7, 19, 10, 0, 0));

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(vo);

        assertTrue(json.contains("\"uv_mom\""), json);
        assertTrue(json.contains("\"week_publish\""), json);
        assertTrue(json.contains("\"response_rate\""), json);
        assertTrue(json.contains("\"hot_contents\""), json);
        assertTrue(json.contains("\"category_dist\""), json);
        assertTrue(json.contains("\"page_heat\""), json);
        assertTrue(json.contains("\"week_publish_trend\""), json);
        assertTrue(json.contains("\"response_rate_trend\""), json);
        assertTrue(json.contains("\"opp_category_pie\""), json);
        assertTrue(json.contains("\"demand_category_pie\""), json);
        assertTrue(json.contains("\"hourly_active\""), json);
        assertTrue(json.contains("\"content_id\""), json);
        assertTrue(json.contains("\"last7d\""), json);
        assertTrue(json.contains("\"2026-07-19 10:00:00\""), json);
        assertFalse(json.contains("hotContents"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("weekPublish"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("2026-07-19T10:00"), "禁 ISO 'T' 分隔：" + json);
    }
}
