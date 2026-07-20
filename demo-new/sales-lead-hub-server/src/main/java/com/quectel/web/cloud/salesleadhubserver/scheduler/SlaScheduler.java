package com.quectel.web.cloud.salesleadhubserver.scheduler;

import com.quectel.web.cloud.salesleadhubserver.dao.AuditLogDao;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestProductLineDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysDepartmentDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.SlaCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SLA 首响时效定时任务：每 5 分钟一次，做两件事——
 * <ol>
 *   <li><b>升级</b>：待响应且无方案的需求，实时等级高于存量 escalation_level 时，
 *       更新存量列 + 通知对应层级负责人 + 写系统审计（operator=system）。</li>
 *   <li><b>自动关闭</b>：待响应超过 7 天（PRD「待响应默认 7 天系统自动关闭」）→ Closed + 通知发布人。</li>
 * </ol>
 *
 * <p><b>幂等</b>：存量列 {@code escalation_level} 即幂等标记——同级不重发（实时等级 &gt; 存量才动作）。</p>
 *
 * <p><b>健壮性</b>：批量小步（LIMIT 200），两阶段各自 try/catch，单次异常只 log 不中断调度；
 * 共享库上跑，避免长事务/全表扫描。</p>
 *
 * <p><b>接收人解析</b>：L1=邀请产品线负责人、L2=部门负责人、L3=平台运营(admin)。
 * 解析逻辑与 {@code SlaServiceImpl.resolveTargetUsers/productLeadUserIds} 同口径，
 * 此处复制一份（调度无 web 上下文，且避免把只读聚合服务改成可被定时任务依赖）。</p>
 */
@Component
public class SlaScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);

    private static final int BATCH_LIMIT = 200;
    private static final int AUTO_CLOSE_DAYS = 7;
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_CLOSED = "Closed";

    private final RequirementDao requirementDao;
    private final NotificationDao notificationDao;
    private final AuditLogDao auditLogDao;
    private final ProductLineMemberDao productLineMemberDao;
    private final RequestProductLineDao requestProductLineDao;
    private final SysDepartmentDao sysDepartmentDao;
    private final SysUserDao sysUserDao;
    private final SlaCalculator calculator;

    public SlaScheduler(RequirementDao requirementDao,
                        NotificationDao notificationDao,
                        AuditLogDao auditLogDao,
                        ProductLineMemberDao productLineMemberDao,
                        RequestProductLineDao requestProductLineDao,
                        SysDepartmentDao sysDepartmentDao,
                        SysUserDao sysUserDao,
                        SlaCalculator calculator) {
        this.requirementDao = requirementDao;
        this.notificationDao = notificationDao;
        this.auditLogDao = auditLogDao;
        this.productLineMemberDao = productLineMemberDao;
        this.requestProductLineDao = requestProductLineDao;
        this.sysDepartmentDao = sysDepartmentDao;
        this.sysUserDao = sysUserDao;
        this.calculator = calculator;
    }

    /** 每 5 分钟（上次结束后再计时）跑一轮升级 + 自动关闭。 */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        try {
            escalate(now);
        } catch (RuntimeException e) {
            log.warn("SLA 升级任务异常(本轮跳过，不影响调度)", e);
        }
        try {
            autoClose(now);
        } catch (RuntimeException e) {
            log.warn("SLA 自动关闭任务异常(本轮跳过，不影响调度)", e);
        }
    }

    /** 升级：实时等级高于存量才动作，同级不重发（幂等）。 */
    public void escalate(LocalDateTime now) {
        List<OpportunityRequestDO> candidates = requirementDao.lambdaQuery()
                .eq(OpportunityRequestDO::getStatus, STATUS_PENDING)
                .eq(OpportunityRequestDO::getResponseCount, 0)
                .last("LIMIT " + BATCH_LIMIT)
                .list();

        for (OpportunityRequestDO d : candidates) {
            int fresh = calculator.escalationLevelInt(d, now);
            int stored = parseLevel(d.getEscalationLevel());
            if (fresh <= stored) {
                continue;   // 同级或更低：不重发
            }
            String newLevel = "L" + fresh;
            // 单列 update（乐观锁外）：只推进 escalation_level 存量标记
            requirementDao.lambdaUpdate()
                    .set(OpportunityRequestDO::getEscalationLevel, newLevel)
                    .eq(OpportunityRequestDO::getId, d.getId())
                    .update();

            Set<Long> targets = resolveEscalationTargets(d, fresh);
            if (!targets.isEmpty()) {
                List<NotificationDO> rows = targets.stream()
                        .map(uid -> systemNotification(uid, "sla_escalate", d.getId(),
                                "需求首响超时升级（" + newLevel + "）：" + d.getTitle()))
                        .collect(Collectors.toList());
                notificationDao.saveBatch(rows);
            }
            writeSystemAudit("sla_escalation", "Request#" + d.getId());
        }
    }

    /** 自动关闭：待响应超过 7 天 → Closed，通知发布人。 */
    public void autoClose(LocalDateTime now) {
        LocalDateTime threshold = now.minusDays(AUTO_CLOSE_DAYS);
        List<OpportunityRequestDO> expired = requirementDao.lambdaQuery()
                .eq(OpportunityRequestDO::getStatus, STATUS_PENDING)
                .lt(OpportunityRequestDO::getCreateTime, threshold)
                .last("LIMIT " + BATCH_LIMIT)
                .list();

        for (OpportunityRequestDO d : expired) {
            requirementDao.lambdaUpdate()
                    .set(OpportunityRequestDO::getStatus, STATUS_CLOSED)
                    .eq(OpportunityRequestDO::getId, d.getId())
                    .update();
            if (d.getPublisherId() != null) {
                notificationDao.save(systemNotification(d.getPublisherId(), "system", d.getId(),
                        "需求待响应超过 " + AUTO_CLOSE_DAYS + " 天，已自动关闭：" + d.getTitle()));
            }
        }
    }

    /**
     * 升级接收人：L1=邀请产品线负责人；L2=部门负责人；L3=平台运营(admin)。
     * 去重保序，取不到某层就返回空（调度不因缺人而中断）。
     */
    public Set<Long> resolveEscalationTargets(OpportunityRequestDO req, int level) {
        Set<Long> ids = new LinkedHashSet<>();
        if (level == 1) {
            ids.addAll(productLeadUserIds(req.getId()));
        } else if (level == 2) {
            if (req.getDepartmentId() != null) {
                SysDepartmentDO dept = sysDepartmentDao.getById(req.getDepartmentId());
                if (dept != null && dept.getOwnerId() != null) {
                    ids.add(dept.getOwnerId());
                }
            }
        } else if (level >= 3) {
            ids.addAll(adminUserIds());
        }
        return ids;
    }

    // ---------- private ----------

    /** 需求邀请产品线的负责人 sys_user id 集（is_owner=1）。 */
    private List<Long> productLeadUserIds(Long requestId) {
        List<Long> lineIds = requestProductLineDao.lambdaQuery()
                .eq(RequestProductLineDO::getRequestId, requestId)
                .list().stream().map(RequestProductLineDO::getProductLineId).collect(Collectors.toList());
        if (lineIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productLineMemberDao.lambdaQuery()
                .eq(ProductLineMemberDO::getIsOwner, 1)
                .in(ProductLineMemberDO::getProductLineId, lineIds)
                .list().stream().map(ProductLineMemberDO::getUserId).collect(Collectors.toList());
    }

    /** 平台运营(admin) 的 sys_user id 集。 */
    private List<Long> adminUserIds() {
        return sysUserDao.lambdaQuery()
                .eq(SysUserDO::getRole, CurrentUserResolver.ROLE_ADMIN)
                .list().stream().map(SysUserDO::getId).collect(Collectors.toList());
    }

    /** 系统触发的站内通知（无触发人姓名，触发者为系统）。 */
    private NotificationDO systemNotification(Long userId, String type, Long requestId, String title) {
        NotificationDO n = new NotificationDO();
        n.setUserId(userId);
        n.setType(type);
        n.setChannel("in_app");
        n.setTargetType("Request");
        n.setTargetId(requestId);
        n.setTitle(title);
        n.setTriggerUserName("system");
        n.setIsRead(0);
        n.setIsForceConfirm(0);
        return n;
    }

    /** 系统操作审计：operator 置系统（id 空、name "system"）。 */
    private void writeSystemAudit(String actionType, String target) {
        AuditLogDO row = new AuditLogDO();
        row.setOperatorId(null);
        row.setOperatorName("system");
        row.setActionType(actionType);
        row.setTarget(target);
        row.setResult("success");
        auditLogDao.save(row);
    }

    /** "L0".."L3" → 0..3；异常/空按 0。 */
    private int parseLevel(String level) {
        if (level == null || level.length() < 2 || level.charAt(0) != 'L') {
            return 0;
        }
        int n = level.charAt(1) - '0';
        return (n < 0 || n > 3) ? 0 : n;
    }
}
