package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeAnnouncementVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeSolutionVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeTaskVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 首页契约测试：各嵌套 VO 序列化——snake_case 键名、安全日期格式、派生标志映射。
 */
class HomeConvertTest {

    private final HomeConvert convert = new HomeConvert();
    private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void solutionVO_snake_case_and_rank_subscribed() throws Exception {
        OpportunityDO d = new OpportunityDO();
        d.setId(1001L);
        d.setTitle("车载 4G+C-V2X 通信模组");
        d.setType("product_info");
        d.setViewCount(1560);
        d.setPublisherName("王强");

        HomeSolutionVO vo = convert.toSolutionVO(d, 1, true);
        String json = om.writeValueAsString(vo);

        assertTrue(json.contains("\"opportunity_id\""), json);
        assertTrue(json.contains("\"view_count\""), json);
        assertTrue(json.contains("\"is_subscribed\":true"), json);
        assertTrue(json.contains("\"publisher_name\""), json);
        assertFalse(json.contains("viewCount"), json);
        assertEquals(Integer.valueOf(1), vo.getRank());
    }

    @Test
    void announcementVO_snake_case_safe_date_and_pinned_flag() throws Exception {
        AnnouncementDO d = new AnnouncementDO();
        d.setId(3001L);
        d.setTitle("平台上线通知");
        d.setType("notice");
        d.setPublisherName("平台运营组");
        d.setViewCount(486);
        d.setIsPinned(1);
        d.setPublishedAt(LocalDateTime.of(2026, 7, 15, 9, 0, 0));

        HomeAnnouncementVO vo = convert.toAnnouncementVO(d);
        String json = om.writeValueAsString(vo);

        assertTrue(json.contains("\"announcement_id\""), json);
        assertTrue(json.contains("\"published_at\""), json);
        assertTrue(json.contains("\"is_pinned\":true"), json);
        assertTrue(json.contains("\"2026-07-15 09:00:00\""), json);
        assertFalse(json.contains("2026-07-15T09:00"), "禁 ISO 'T' 分隔：" + json);
    }

    @Test
    void taskVO_snake_case_safe_date_and_overdue_flag() throws Exception {
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(2001L);
        d.setTaskDesc("整理竞品对比材料");
        d.setMeetingName("产品周会");
        d.setPriority("critical");
        d.setStatus("pending");
        d.setDeadline(LocalDateTime.of(2026, 7, 16, 18, 0, 0));

        HomeTaskVO vo = convert.toTaskVO(d, true);
        String json = om.writeValueAsString(vo);

        assertTrue(json.contains("\"task_id\""), json);
        assertTrue(json.contains("\"meeting_name\""), json);
        assertTrue(json.contains("\"is_overdue\":true"), json);
        assertTrue(json.contains("\"2026-07-16 18:00:00\""), json);
        assertEquals("整理竞品对比材料", vo.getTitle());
        assertFalse(json.contains("meetingName"), json);
    }
}
