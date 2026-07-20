package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.MeetingTaskConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.BatchPublishDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.BatchTaskDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.BatchServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批量发布 service 行为测试：仅 admin、复用会议、执行人校验、一次多任务落库。
 * dao 全 mock，离线可跑。
 */
class BatchServiceImplTest {

    @Mock MeetingDao meetingDao;
    @Mock MeetingTaskDao taskDao;
    @Mock SysUserDao sysUserDao;
    @Mock CurrentUserResolver currentUser;

    private BatchServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new BatchServiceImpl(meetingDao, taskDao, sysUserDao, currentUser, new MeetingTaskConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(long id, String name) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName(name);
        u.setStatus("active");
        u.setDepartmentName("无线模组产品部");
        return u;
    }

    private BatchPublishDTO existSourceDto(List<String> executorIds) {
        BatchTaskDTO t = new BatchTaskDTO();
        t.setDesc("任务一");
        t.setPriority("urgent");
        t.setDeadline("2026-07-20");
        t.setExecutorIds(executorIds);
        BatchPublishDTO dto = new BatchPublishDTO();
        dto.setMeetingSource("exist");
        dto.setMeetingId(97001L);
        dto.setTasks(new ArrayList<>(Collections.singletonList(t)));
        return dto;
    }

    @Test
    void publish_rejects_missing_executor() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9001L, "运营管理员"));
        MeetingDO m = new MeetingDO();
        m.setId(97001L);
        when(meetingDao.getById(97001L)).thenReturn(m);
        when(sysUserDao.getById(9999L)).thenReturn(null);   // 执行人不存在

        assertThrows(BaseException.class,
                () -> service.publish(existSourceDto(Collections.singletonList("9999"))));
        verify(taskDao, never()).save(any());
    }

    @Test
    void publish_exist_meeting_snapshots_executor_names() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9001L, "运营管理员"));
        MeetingDO m = new MeetingDO();
        m.setId(97001L);
        m.setName("7月产品线周例会");
        m.setRecorderName("张伟");
        when(meetingDao.getById(97001L)).thenReturn(m);
        when(sysUserDao.getById(9002L)).thenReturn(user(9002L, "张伟"));
        when(sysUserDao.getById(9003L)).thenReturn(user(9003L, "李娜"));

        service.publish(existSourceDto(Arrays.asList("9002", "9003")));

        ArgumentCaptor<MeetingTaskDO> cap = ArgumentCaptor.forClass(MeetingTaskDO.class);
        verify(taskDao).save(cap.capture());
        MeetingTaskDO saved = cap.getValue();
        assertEquals(Long.valueOf(97001L), saved.getMeetingId());
        assertEquals(Arrays.asList(9002L, 9003L), saved.getAssigneeIds());
        assertEquals(Arrays.asList("张伟", "李娜"), saved.getAssigneeNames());
        assertEquals("pending", saved.getStatus());
        verify(meetingDao, never()).save(any());   // 复用，不新建
    }

    @Test
    void publish_exist_missing_meeting_is_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9001L, "运营管理员"));
        when(meetingDao.getById(97001L)).thenReturn(null);

        assertThrows(BaseException.class,
                () -> service.publish(existSourceDto(Collections.singletonList("9002"))));
        verify(taskDao, never()).save(any());
    }

    @Test
    void publish_new_meeting_inserts_meeting_and_all_tasks() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9001L, "运营管理员"));
        when(sysUserDao.getById(9002L)).thenReturn(user(9002L, "张伟"));

        BatchTaskDTO t1 = new BatchTaskDTO();
        t1.setDesc("任务一");
        t1.setPriority("normal");
        t1.setDeadline("2026-07-20");
        t1.setExecutorIds(Collections.singletonList("9002"));
        BatchTaskDTO t2 = new BatchTaskDTO();
        t2.setDesc("任务二");
        t2.setPriority("urgent");
        t2.setDeadline("2026-07-21");
        t2.setExecutorIds(Collections.singletonList("9002"));

        BatchPublishDTO dto = new BatchPublishDTO();
        dto.setMeetingSource("new");
        dto.setMeetingName("临时评审会");
        dto.setMeetingDate("2026-07-18");
        dto.setRecorderName("李娜");
        dto.setTasks(Arrays.asList(t1, t2));

        service.publish(dto);

        verify(meetingDao).save(any(MeetingDO.class));       // 新建会议
        verify(taskDao, times(2)).save(any(MeetingTaskDO.class));   // 两条任务
    }
}
