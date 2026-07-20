package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.MeetingTaskConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.MeetingServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会议任务管理 service 行为测试：鉴权/owner 边界、会议复用、状态机非法跃迁。
 * dao 全 mock，离线可跑。
 */
class MeetingServiceImplTest {

    @Mock MeetingDao meetingDao;
    @Mock MeetingTaskDao taskDao;
    @Mock CurrentUserResolver currentUser;

    private MeetingServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new MeetingServiceImpl(meetingDao, taskDao, currentUser, new MeetingTaskConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(long id, String role) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName("用户" + id);
        u.setRole(role);
        u.setStatus("active");
        return u;
    }

    private MeetingCreateDTO createDto() {
        MeetingCreateDTO dto = new MeetingCreateDTO();
        dto.setMeetingName("5G RedCap 周例会");
        dto.setMeetingDate("2026-07-08");
        dto.setRecorderName("张伟");
        dto.setTaskDesc("整理功耗测试报告");
        dto.setPriority("urgent");
        dto.setDeadline("2026-07-12 18:00:00");
        dto.setAssigneeNames(Arrays.asList("李娜", "王强"));
        return dto;
    }

    // ---------- create ----------

    @Test
    void create_denied_propagates_and_never_saves() {
        when(currentUser.requireAnyRole(any()))
                .thenThrow(new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        assertThrows(BaseException.class, () -> service.create(createDto()));
        verify(taskDao, never()).save(any());
    }

    @Test
    void create_reuses_existing_meeting_and_snapshots_task() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        MeetingDO existing = new MeetingDO();
        existing.setId(97001L);
        existing.setName("5G RedCap 周例会");
        when(meetingDao.getOne(any())).thenReturn(existing);   // 命中已有会议

        service.create(createDto());

        // 复用会议行，不再新建 meeting
        verify(meetingDao, never()).save(any());
        ArgumentCaptor<MeetingTaskDO> cap = ArgumentCaptor.forClass(MeetingTaskDO.class);
        verify(taskDao).save(cap.capture());
        MeetingTaskDO saved = cap.getValue();
        assertEquals(Long.valueOf(97001L), saved.getMeetingId());
        assertEquals("5G RedCap 周例会", saved.getMeetingName());
        assertEquals("pending", saved.getStatus());
        assertEquals(Arrays.asList("李娜", "王强"), saved.getAssigneeNames());
    }

    @Test
    void create_new_meeting_when_none_matched() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        when(meetingDao.getOne(any())).thenReturn(null);   // 无匹配会议

        service.create(createDto());

        verify(meetingDao).save(any(MeetingDO.class));   // 新建会议行
        verify(taskDao).save(any(MeetingTaskDO.class));
    }

    // ---------- update ----------

    @Test
    void update_by_stranger_is_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9004L, CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setCreateBy(9002L);   // 别人创建的
        when(taskDao.getById(1L)).thenReturn(d);

        MeetingUpdateDTO dto = new MeetingUpdateDTO();
        dto.setId(1L);
        dto.setMeetingName("x");
        dto.setMeetingDate("2026-07-08");
        dto.setTaskDesc("y");
        dto.setPriority("normal");
        dto.setDeadline("2026-07-12 18:00:00");
        dto.setAssigneeNames(Collections.singletonList("李娜"));

        assertThrows(BaseException.class, () -> service.update(dto));
        verify(taskDao, never()).updateById(any());
    }

    // ---------- cancel ----------

    @Test
    void cancel_completed_task_is_illegal_transition() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setCreateBy(9002L);
        d.setStatus("completed");
        when(taskDao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.cancel(1L, "原因"));
        verify(taskDao, never()).updateById(any());
    }

    @Test
    void cancel_pending_task_records_reason() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setCreateBy(9002L);
        d.setStatus("pending");
        when(taskDao.getById(1L)).thenReturn(d);
        when(taskDao.updateById(any())).thenReturn(true);

        service.cancel(1L, "需求取消");

        assertEquals("cancelled", d.getStatus());
        assertEquals("需求取消", d.getCancelReason());
    }
}
