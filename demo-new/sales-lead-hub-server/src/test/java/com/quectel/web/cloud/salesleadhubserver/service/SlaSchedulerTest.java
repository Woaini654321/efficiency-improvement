package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.quectel.web.cloud.salesleadhubserver.dao.AuditLogDao;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysDepartmentDO;
import com.quectel.web.cloud.salesleadhubserver.scheduler.SlaScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SLA 定时任务单测：升级幂等（同级不重发）、升级触发通知、自动关闭处理、升级接收人解析。
 *
 * <p>SlaCalculator 用真实实例（纯函数）；DB 侧过滤已由 mock 链短路，故本测聚焦「取到候选后」的行为。</p>
 */
class SlaSchedulerTest {

    private RequirementDao requirementDao;
    private NotificationDao notificationDao;
    private AuditLogDao auditLogDao;
    private ProductLineMemberDao productLineMemberDao;
    private RequestProductLineDao requestProductLineDao;
    private SysDepartmentDao sysDepartmentDao;
    private SysUserDao sysUserDao;
    private SlaScheduler scheduler;

    @BeforeEach
    void init() {
        requirementDao = mock(RequirementDao.class);
        notificationDao = mock(NotificationDao.class);
        auditLogDao = mock(AuditLogDao.class);
        productLineMemberDao = mock(ProductLineMemberDao.class);
        requestProductLineDao = mock(RequestProductLineDao.class);
        sysDepartmentDao = mock(SysDepartmentDao.class);
        sysUserDao = mock(SysUserDao.class);
        scheduler = new SlaScheduler(requirementDao, notificationDao, auditLogDao,
                productLineMemberDao, requestProductLineDao, sysDepartmentDao, sysUserDao,
                new SlaCalculator());
    }

    @SuppressWarnings("unchecked")
    private void stubCandidates(List<OpportunityRequestDO> list) {
        LambdaQueryChainWrapper<OpportunityRequestDO> chain =
                mock(LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        when(chain.list()).thenReturn(list);
        when(requirementDao.lambdaQuery()).thenReturn(chain);
    }

    @SuppressWarnings("unchecked")
    private void stubUpdateChain() {
        LambdaUpdateChainWrapper<OpportunityRequestDO> upd =
                mock(LambdaUpdateChainWrapper.class, Answers.RETURNS_SELF);
        when(upd.update()).thenReturn(true);
        when(requirementDao.lambdaUpdate()).thenReturn(upd);
    }

    private OpportunityRequestDO pending(Long id, String urgency, LocalDateTime createTime, String storedLevel) {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(id);
        d.setStatus("Pending");
        d.setUrgency(urgency);
        d.setResponseCount(0);
        d.setCreateTime(createTime);
        d.setEscalationLevel(storedLevel);
        d.setTitle("需求" + id);
        d.setPublisherId(9002L);
        d.setDepartmentId(1001L);
        return d;
    }

    // —— 升级幂等 ——

    @Test
    void escalate_skips_when_stored_level_already_current() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 19, 12, 0);
        // normal 阈值 24h；createTime = now-30h → 实时 L1；存量已 L1 → 同级不重发
        stubCandidates(Collections.singletonList(
                pending(1L, "normal", now.minusHours(30), "L1")));

        scheduler.escalate(now);

        verify(requirementDao, never()).lambdaUpdate();
        verify(notificationDao, never()).saveBatch(any());
        verify(auditLogDao, never()).save(any());
    }

    @Test
    void escalate_promotes_and_notifies_and_audits_when_level_rises() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 19, 12, 0);
        // normal 阈值 24h；createTime = now-50h → 实时 L2（50/24=2）；存量 L0 → 升级
        stubCandidates(Collections.singletonList(
                pending(1L, "normal", now.minusHours(50), "L0")));
        stubUpdateChain();
        // L2 接收人 = 部门负责人
        SysDepartmentDO dept = new SysDepartmentDO();
        dept.setId(1001L);
        dept.setOwnerId(6001L);
        when(sysDepartmentDao.getById(1001L)).thenReturn(dept);

        scheduler.escalate(now);

        verify(requirementDao).lambdaUpdate();               // 推进存量 escalation_level
        verify(notificationDao).saveBatch(any());            // 通知部门负责人
        verify(auditLogDao).save(any());                     // 系统审计 sla_escalation
    }

    // —— 自动关闭 ——

    @Test
    void autoClose_closes_expired_and_notifies_publisher() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 19, 12, 0);
        // 返回一条已超 7 天的待响应需求（DB 过滤已由 mock 短路，这里给到即视为过期）
        stubCandidates(Collections.singletonList(
                pending(2L, "normal", now.minusDays(8), "L0")));
        stubUpdateChain();

        scheduler.autoClose(now);

        verify(requirementDao).lambdaUpdate();           // 置 Closed
        ArgumentCaptor<NotificationDO> cap = ArgumentCaptor.forClass(NotificationDO.class);
        verify(notificationDao).save(cap.capture());
        assertEquals(Long.valueOf(9002L), cap.getValue().getUserId(), "通知发布人");
        assertEquals("system", cap.getValue().getType());
    }

    // —— 接收人解析 ——

    @Test
    void resolveEscalationTargets_level2_returns_department_owner() {
        SysDepartmentDO dept = new SysDepartmentDO();
        dept.setId(1001L);
        dept.setOwnerId(6001L);
        when(sysDepartmentDao.getById(1001L)).thenReturn(dept);

        OpportunityRequestDO req = pending(3L, "normal", LocalDateTime.now(), "L0");
        Set<Long> targets = scheduler.resolveEscalationTargets(req, 2);

        assertTrue(targets.contains(6001L), "L2 接收人应为部门负责人");
        assertEquals(1, targets.size());
    }

    @Test
    void resolveEscalationTargets_level2_empty_when_no_dept_owner() {
        when(sysDepartmentDao.getById(1001L)).thenReturn(null);
        OpportunityRequestDO req = pending(4L, "normal", LocalDateTime.now(), "L0");
        assertTrue(scheduler.resolveEscalationTargets(req, 2).isEmpty());
    }
}
