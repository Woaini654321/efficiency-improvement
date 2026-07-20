package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaRequestVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaTimelineVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SLA 契约测试：VO 离线序列化门禁——snake_case 键名、安全日期格式、无 camelCase。
 */
class SlaConvertTest {

    private final SlaConvert convert = new SlaConvert();

    @Test
    void requestVO_serializes_snake_case_safe_date_and_nested_timeline() throws Exception {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(123456789012345678L);
        d.setTitle("求高精度定位模组车规级选型方案");
        d.setUrgency("critical");
        d.setResponseCount(0);
        d.setPublisherName("张伟");
        d.setCreateTime(LocalDateTime.of(2027, 1, 15, 7, 0, 0));

        SlaTimelineVO row = convert.timelineRow("01-15 07:00", "需求发布，计时开始", "发布人 张伟");
        SlaRequestVO vo = convert.toRequestVO(d, "overdue", "L2", "已超时 3时0分",
                LocalDateTime.of(2027, 1, 15, 9, 0, 0), Collections.singletonList(row));

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(vo);

        assertTrue(json.contains("\"request_id\""), json);
        assertTrue(json.contains("\"sla_status\""), json);
        assertTrue(json.contains("\"remaining_text\""), json);
        assertTrue(json.contains("\"escalation_level\""), json);
        assertTrue(json.contains("\"publisher_name\""), json);
        assertTrue(json.contains("\"escalation_timeline\""), json);
        assertTrue(json.contains("\"notify_to\""), json);
        assertFalse(json.contains("slaStatus"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("remainingText"), "不得出现 camelCase：" + json);
        // 默认 ISO 'T' 会让前端 new Date(s.replace(' ','T')) 之前的展示错乱，须空格分隔
        assertTrue(json.contains("\"2027-01-15 07:00:00\""), json);
        assertTrue(json.contains("\"2027-01-15 09:00:00\""), json);
        assertFalse(json.contains("2027-01-15T07:00"), "禁 ISO 'T' 分隔：" + json);
    }
}
