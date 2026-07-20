package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.TransferRecord;
import com.quectel.web.cloud.salesleadhubserver.vo.MeetingTaskPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.TaskPageVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 会议任务模块契约测试：VO 离线序列化门禁（snake_case 键 + 安全日期格式 +
 * assignee_names 数组 + transfer_history 嵌套）。纯 ObjectMapper 即可钉死，无需联调。
 */
class MeetingTaskConvertTest {

    private final MeetingTaskConvert convert = new MeetingTaskConvert();

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MeetingTaskDO sampleDO() {
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(123456789012345678L);
        d.setMeetingId(97001L);
        d.setMeetingName("5G RedCap 产品线周例会");
        d.setMeetingDate(LocalDateTime.of(2026, 7, 8, 0, 0, 0));
        d.setRecorderName("张伟");
        d.setTaskDesc("整理功耗测试报告");
        d.setPriority("urgent");
        d.setDeadline(LocalDateTime.of(2026, 7, 12, 18, 0, 0));
        d.setStatus("processing");
        d.setAssigneeNames(Arrays.asList("李娜", "王强"));
        d.setCreateTime(LocalDateTime.of(2026, 7, 8, 10, 30, 0));
        return d;
    }

    @Test
    void pageVO_serializes_snake_case_safe_date_and_assignee_names() throws Exception {
        MeetingTaskPageVO vo = convert.toPageVO(sampleDO());
        String json = mapper.writeValueAsString(vo);

        // 前端 adapter 读的是 task_id，不是 id
        assertTrue(json.contains("\"task_id\""), json);
        assertTrue(json.contains("\"meeting_name\""), json);
        assertTrue(json.contains("\"recorder_name\""), json);
        assertTrue(json.contains("\"task_desc\""), json);
        assertTrue(json.contains("\"assignee_names\""), json);
        assertTrue(json.contains("\"created_at\""), json);
        assertFalse(json.contains("meetingName"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("assigneeNames"), "不得出现 camelCase：" + json);
        // 默认 ISO 的 'T' 分隔会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
        assertTrue(json.contains("\"2026-07-12 18:00:00\""), json);
        assertFalse(json.contains("2026-07-12T18:00"), "禁 ISO 'T' 分隔：" + json);
        // assignee_names 是数组
        assertTrue(json.contains("[\"李娜\",\"王强\"]"), json);
    }

    @Test
    void taskVO_serializes_transfer_from_and_nested_transfer_history() throws Exception {
        MeetingTaskDO d = sampleDO();
        d.setStatus("transferred");
        d.setTransferFrom("陈涛");
        TransferRecord rec = new TransferRecord();
        rec.setTime("2026-07-03 11:40:00");
        rec.setFrom("陈涛");
        rec.setTo("王强");
        rec.setReason("客户现场问题需就近跟进");
        d.setTransferHistory(Collections.singletonList(rec));

        TaskPageVO vo = convert.toTaskVO(d);
        String json = mapper.writeValueAsString(vo);

        assertTrue(json.contains("\"transfer_from\":\"陈涛\""), json);
        assertTrue(json.contains("\"transfer_history\""), json);
        // 嵌套 TransferRecord 字段全小写单词，snake 化 no-op，键应逐字出现
        assertTrue(json.contains("\"time\":\"2026-07-03 11:40:00\""), json);
        assertTrue(json.contains("\"from\":\"陈涛\""), json);
        assertTrue(json.contains("\"to\":\"王强\""), json);
        assertTrue(json.contains("\"reason\":\"客户现场问题需就近跟进\""), json);
        assertFalse(json.contains("transferFrom"), "不得出现 camelCase：" + json);
    }

    @Test
    void taskVO_null_transferFrom_becomes_empty_string() {
        MeetingTaskDO d = sampleDO();
        d.setTransferFrom(null);
        TaskPageVO vo = convert.toTaskVO(d);
        // 前端 adapter 用 ?? '' 兜底，后端也归一为空串，语义「非转交」
        assertEquals("", vo.getTransferFrom());
    }

    @Test
    void parseDateTime_handles_date_only_and_datetime_and_null() {
        assertEquals(LocalDateTime.of(2026, 7, 8, 0, 0, 0),
                MeetingTaskConvert.parseDateTime("2026-07-08"));
        assertEquals(LocalDateTime.of(2026, 7, 12, 18, 0, 0),
                MeetingTaskConvert.parseDateTime("2026-07-12 18:00:00"));
        assertEquals(LocalDateTime.of(2026, 7, 12, 18, 5, 0),
                MeetingTaskConvert.parseDateTime("2026-07-12 18:05"));
        assertNull(MeetingTaskConvert.parseDateTime(null));
        assertNull(MeetingTaskConvert.parseDateTime("  "));
    }
}
