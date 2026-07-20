package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.MeetingTaskConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.TaskTransferDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 「我的任务」service 行为测试：assignee 边界、状态机非法跃迁、转交换人+追加历史。
 * dao 全 mock，离线可跑。
 */
class TaskServiceImplTest {

    @Mock MeetingTaskDao taskDao;
    @Mock SysUserDao sysUserDao;
    @Mock CurrentUserResolver currentUser;

    private TaskServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new TaskServiceImpl(taskDao, sysUserDao, currentUser, new MeetingTaskConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(long id, String name, String role) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        u.setStatus("active");
        return u;
    }

    // ---------- assignee 边界 ----------

    @Test
    void start_by_non_assignee_is_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9004L, "王强", CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setStatus("pending");
        d.setAssigneeNames(Collections.singletonList("李娜"));   // 不含王强
        when(taskDao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.start(1L));
        verify(taskDao, never()).updateById(any());
    }

    // ---------- 状态机 ----------

    @Test
    void start_non_pending_is_illegal_transition() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9003L, "李娜", CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setStatus("completed");
        d.setAssigneeNames(Collections.singletonList("李娜"));
        when(taskDao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.start(1L));
        verify(taskDao, never()).updateById(any());
    }

    @Test
    void start_pending_by_assignee_moves_to_processing() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9003L, "李娜", CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setStatus("pending");
        d.setAssigneeNames(Collections.singletonList("李娜"));
        when(taskDao.getById(1L)).thenReturn(d);
        when(taskDao.updateById(any())).thenReturn(true);

        service.start(1L);
        assertEquals("processing", d.getStatus());
    }

    @Test
    void complete_from_pending_or_processing_is_allowed() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9003L, "李娜", CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setStatus("processing");
        d.setAssigneeNames(Collections.singletonList("李娜"));
        when(taskDao.getById(1L)).thenReturn(d);
        when(taskDao.updateById(any())).thenReturn(true);

        service.complete(1L, "已交付");
        assertEquals("completed", d.getStatus());
        assertEquals("已交付", d.getCompleteRemark());
    }

    // ---------- 转交 ----------

    @Test
    void transfer_appends_history_and_swaps_executor() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9003L, "李娜", CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setStatus("processing");
        d.setAssigneeNames(Collections.singletonList("李娜"));
        when(taskDao.getById(1L)).thenReturn(d);
        when(taskDao.updateById(any())).thenReturn(true);

        TaskTransferDTO dto = new TaskTransferDTO();
        dto.setId(1L);
        dto.setTransferTo("王强");   // 姓名（非数字）→ 不查 sys_user
        dto.setReason("区域交接");
        service.transfer(dto);

        assertEquals("transferred", d.getStatus());
        assertEquals("李娜", d.getTransferFrom());
        assertEquals(Collections.singletonList("王强"), d.getAssigneeNames());
        assertEquals(1, d.getTransferHistory().size());
        assertEquals("李娜", d.getTransferHistory().get(0).getFrom());
        assertEquals("王强", d.getTransferHistory().get(0).getTo());
        assertEquals("区域交接", d.getTransferHistory().get(0).getReason());
        assertTrue(d.getAssigneeIds().isEmpty(), "姓名转交无 id，assignee_ids 应为空");
    }

    @Test
    void transfer_numeric_target_resolves_user_and_rejects_inactive() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9003L, "李娜", CurrentUserResolver.ROLE_SALES));
        MeetingTaskDO d = new MeetingTaskDO();
        d.setId(1L);
        d.setStatus("processing");
        d.setAssigneeNames(Collections.singletonList("李娜"));
        when(taskDao.getById(1L)).thenReturn(d);
        when(sysUserDao.getById(9999L)).thenReturn(null);   // 目标不存在

        TaskTransferDTO dto = new TaskTransferDTO();
        dto.setId(1L);
        dto.setTransferTo("9999");   // 纯数字 → 按 id 解析
        dto.setReason("x");

        assertThrows(BaseException.class, () -> service.transfer(dto));
        verify(taskDao, never()).updateById(any());
    }

    @Test
    void task_not_found_raises() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9003L, "李娜", CurrentUserResolver.ROLE_SALES));
        when(taskDao.getById(1L)).thenReturn(null);
        BaseException ex = assertThrows(BaseException.class, () -> service.start(1L));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
    }
}
