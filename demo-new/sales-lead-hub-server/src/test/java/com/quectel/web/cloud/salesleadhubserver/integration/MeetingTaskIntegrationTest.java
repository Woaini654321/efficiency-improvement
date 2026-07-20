package com.quectel.web.cloud.salesleadhubserver.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.TransferRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 会议任务模块真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见
 * OpportunityIntegrationTest）。
 *
 * <p>直连 DAO（绕开 service 的 SSO 鉴权），覆盖：JSON 列（assignee_ids/assignee_names/
 * transfer_history）经 JacksonTypeHandler 往返、状态流转落库、会议复用查询语义、
 * 批量一次多任务落库。测试数据自建自删。</p>
 *
 * <p>反选执行：{@code mvn test -Dtest=MeetingTaskIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，Tests run: 0 视为 FAIL。</p>
 */
@Tag("integration")
@SpringBootTest
class MeetingTaskIntegrationTest {

    @Autowired
    MeetingDao meetingDao;

    @Autowired
    MeetingTaskDao taskDao;

    /** JSON 列往返 + 状态流转落库。 */
    @Test
    void json_columns_roundtrip_and_status_flow() {
        MeetingTaskDO d = new MeetingTaskDO();
        d.setMeetingId(1L);
        d.setMeetingName("集成测试会议");
        d.setMeetingDate(LocalDateTime.of(2026, 7, 8, 0, 0, 0));
        d.setRecorderName("张伟");
        d.setTaskDesc("集成测试任务");
        d.setPriority("urgent");
        d.setDeadline(LocalDateTime.of(2026, 7, 12, 18, 0, 0));
        d.setStatus("pending");
        d.setAssigneeIds(Arrays.asList(9002L, 9003L));
        d.setAssigneeNames(Arrays.asList("张伟", "李娜"));
        TransferRecord rec = new TransferRecord();
        rec.setTime("2026-07-09 10:00:00");
        rec.setFrom("张伟");
        rec.setTo("李娜");
        rec.setReason("交接");
        d.setTransferHistory(Collections.singletonList(rec));
        taskDao.save(d);
        Long id = d.getId();
        assertNotNull(id, "雪花 id 应回填");

        try {
            // JSON 列经 JacksonTypeHandler 往返应结构一致
            MeetingTaskDO loaded = taskDao.getById(id);
            assertEquals(Arrays.asList(9002L, 9003L), loaded.getAssigneeIds());
            assertEquals(Arrays.asList("张伟", "李娜"), loaded.getAssigneeNames());
            assertEquals(1, loaded.getTransferHistory().size());
            assertEquals("交接", loaded.getTransferHistory().get(0).getReason());
            assertEquals("李娜", loaded.getTransferHistory().get(0).getTo());

            // 状态流转落库：pending → processing → completed
            loaded.setStatus("processing");
            taskDao.updateById(loaded);
            assertEquals("processing", taskDao.getById(id).getStatus());

            loaded.setStatus("completed");
            loaded.setCompleteRemark("已交付");
            taskDao.updateById(loaded);
            MeetingTaskDO done = taskDao.getById(id);
            assertEquals("completed", done.getStatus());
            assertEquals("已交付", done.getCompleteRemark());
        } finally {
            taskDao.removeById(id);
        }
    }

    /** meeting/create 复用会议：按 (name, meeting_date) 查询命中同一行。 */
    @Test
    void meeting_reuse_lookup_matches_same_row() {
        LocalDateTime date = LocalDateTime.of(2026, 7, 15, 0, 0, 0);
        MeetingDO m = new MeetingDO();
        m.setName("集成测试复用会议");
        m.setMeetingDate(date);
        m.setRecorderName("张伟");
        meetingDao.save(m);
        Long id = m.getId();
        assertNotNull(id);

        try {
            MeetingDO found = meetingDao.getOne(new LambdaQueryWrapper<MeetingDO>()
                    .eq(MeetingDO::getName, "集成测试复用会议")
                    .eq(MeetingDO::getMeetingDate, date)
                    .last("limit 1"));
            assertNotNull(found, "按 name+date 应查到已建会议");
            assertEquals(id, found.getId(), "复用应命中同一会议行");
        } finally {
            meetingDao.removeById(id);
        }
    }

    /** batch publish：一次多任务落库，均挂在同一会议下。 */
    @Test
    void batch_publish_inserts_multiple_tasks_under_one_meeting() {
        MeetingDO m = new MeetingDO();
        m.setName("集成测试批量会议");
        m.setMeetingDate(LocalDateTime.of(2026, 7, 16, 0, 0, 0));
        m.setRecorderName("李娜");
        meetingDao.save(m);
        Long meetingId = m.getId();

        List<Long> taskIds = new ArrayList<>();
        try {
            for (int i = 1; i <= 3; i++) {
                MeetingTaskDO d = new MeetingTaskDO();
                d.setMeetingId(meetingId);
                d.setMeetingName(m.getName());
                d.setMeetingDate(m.getMeetingDate());
                d.setRecorderName(m.getRecorderName());
                d.setTaskDesc("批量任务" + i);
                d.setPriority("normal");
                d.setDeadline(LocalDateTime.of(2026, 7, 20, 18, 0, 0));
                d.setStatus("pending");
                d.setAssigneeIds(Collections.singletonList(9002L));
                d.setAssigneeNames(Collections.singletonList("张伟"));
                d.setTransferFrom("");
                d.setTransferHistory(Collections.emptyList());
                taskDao.save(d);
                taskIds.add(d.getId());
            }

            long count = taskDao.count(new LambdaQueryWrapper<MeetingTaskDO>()
                    .eq(MeetingTaskDO::getMeetingId, meetingId));
            assertEquals(3L, count, "同一会议下应落 3 条任务");
        } finally {
            for (Long tid : taskIds) {
                taskDao.removeById(tid);
            }
            meetingDao.removeById(meetingId);
        }
    }
}
