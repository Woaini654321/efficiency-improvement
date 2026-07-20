package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.vo.NotificationPageVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通知模块契约测试：DO→VO 映射 + VO 离线序列化门禁。
 *
 * <p>纯 ObjectMapper 即可钉死 snake_case 键名、日期格式与「不出现 camelCase」，无需联调。</p>
 */
class NotificationConvertTest {

    private final NotificationConvert convert = new NotificationConvert();

    @Test
    void toPageVO_maps_flags_from_tinyint_to_boolean() {
        NotificationDO d = new NotificationDO();
        d.setId(2001L);
        d.setType("response");
        d.setChannel("in_app");
        d.setTitle("您发布的需求收到了新的方案响应");
        d.setTriggerUserName("李娜");
        d.setIsRead(0);
        d.setIsForceConfirm(1);
        d.setTargetType("requirement");
        d.setTargetId(3001L);

        NotificationPageVO v = convert.toPageVO(d);

        assertEquals(Boolean.FALSE, v.getIsRead());
        assertEquals(Boolean.TRUE, v.getIsForceConfirm());
        assertEquals(Long.valueOf(2001L), v.getNotificationId());
        assertEquals("李娜", v.getTriggerUserName());
    }

    @Test
    void pageVO_serializes_snake_case_and_safe_date() throws Exception {
        NotificationDO d = new NotificationDO();
        d.setId(2002L);
        d.setType("adopt");
        d.setChannel("in_app");
        d.setTitle("您提交的方案已被采纳");
        d.setTriggerUserName("赵敏");
        d.setIsRead(1);
        d.setIsForceConfirm(0);
        d.setTargetType("requirement");
        d.setTargetId(3002L);
        d.setCreateTime(LocalDateTime.of(2026, 7, 18, 8, 45, 0));

        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(convert.toPageVO(d));

        // 前端 adapter 读的键名是 notification_id / trigger_user_name / is_read / target_type / created_at
        assertTrue(json.contains("\"notification_id\""), json);
        assertTrue(json.contains("\"trigger_user_name\""), json);
        assertTrue(json.contains("\"is_read\""), json);
        assertTrue(json.contains("\"is_force_confirm\""), json);
        assertTrue(json.contains("\"target_type\""), json);
        assertTrue(json.contains("\"target_id\""), json);
        assertFalse(json.contains("triggerUserName"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("notificationId"), "不得出现 camelCase：" + json);
        // 默认 ISO 的 'T' 分隔会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
        assertTrue(json.contains("\"2026-07-18 08:45:00\""), json);
        assertFalse(json.contains("2026-07-18T08:45"), "禁 ISO 'T' 分隔：" + json);
    }
}
