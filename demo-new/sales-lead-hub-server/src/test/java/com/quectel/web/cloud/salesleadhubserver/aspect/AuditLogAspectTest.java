package com.quectel.web.cloud.salesleadhubserver.aspect;

import com.quectel.web.cloud.salesleadhubserver.annotation.AuditAction;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审计切面单测：成功写 success 行、失败写 fail 行并原样抛出、写审计失败不影响主流程。
 *
 * <p>直接构造切面并手动调用 {@code around(pjp, annotation)}，不起 Spring AOP。</p>
 */
class AuditLogAspectTest {

    private CurrentUserResolver currentUser;
    private AuditLogWriter writer;
    private AuditLogAspect aspect;

    @BeforeEach
    void init() {
        currentUser = mock(CurrentUserResolver.class);
        writer = mock(AuditLogWriter.class);
        aspect = new AuditLogAspect(currentUser, writer);

        SysUserDO me = new SysUserDO();
        me.setId(10160L);
        me.setName("运营");
        when(currentUser.currentOrNull()).thenReturn(me);
    }

    private AuditAction annotation(String actionType, String targetType) {
        AuditAction a = mock(AuditAction.class);
        when(a.actionType()).thenReturn(actionType);
        when(a.targetType()).thenReturn(targetType);
        return a;
    }

    private ProceedingJoinPoint pjpWithId(Long id) {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{id});
        return pjp;
    }

    @Test
    void success_writes_success_row() throws Throwable {
        ProceedingJoinPoint pjp = pjpWithId(777L);
        when(pjp.proceed()).thenReturn("ok");

        Object ret = aspect.around(pjp, annotation("delete", "Content"));

        assertEquals("ok", ret);
        ArgumentCaptor<AuditLogDO> cap = ArgumentCaptor.forClass(AuditLogDO.class);
        verify(writer).write(cap.capture());
        AuditLogDO row = cap.getValue();
        assertEquals("success", row.getResult());
        assertEquals("delete", row.getActionType());
        assertEquals("Content#777", row.getTarget());
        assertEquals(Long.valueOf(10160L), row.getOperatorId());
        assertEquals("运营", row.getOperatorName());
    }

    @Test
    void auto_status_derives_archive_from_arg() throws Throwable {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.getArgs()).thenReturn(new Object[]{123L, "archived"});
        when(pjp.proceed()).thenReturn(null);

        aspect.around(pjp, annotation(AuditAction.AUTO_STATUS, "Opportunity"));

        ArgumentCaptor<AuditLogDO> cap = ArgumentCaptor.forClass(AuditLogDO.class);
        verify(writer).write(cap.capture());
        assertEquals("archive", cap.getValue().getActionType(), "archived 入参应派生 archive");
    }

    @Test
    void failure_writes_fail_row_and_rethrows() throws Throwable {
        ProceedingJoinPoint pjp = pjpWithId(888L);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class,
                () -> aspect.around(pjp, annotation("delete", "Content")));

        ArgumentCaptor<AuditLogDO> cap = ArgumentCaptor.forClass(AuditLogDO.class);
        verify(writer).write(cap.capture());
        assertEquals("fail", cap.getValue().getResult(), "异常路径写 fail 行");
    }

    @Test
    void writer_failure_does_not_break_main_flow() throws Throwable {
        ProceedingJoinPoint pjp = pjpWithId(999L);
        when(pjp.proceed()).thenReturn("ok");
        doThrow(new RuntimeException("audit db down")).when(writer).write(any());

        // 写审计抛异常被切面吞掉，主流程返回值不受影响
        Object ret = aspect.around(pjp, annotation("delete", "Content"));
        assertEquals("ok", ret);
    }
}
