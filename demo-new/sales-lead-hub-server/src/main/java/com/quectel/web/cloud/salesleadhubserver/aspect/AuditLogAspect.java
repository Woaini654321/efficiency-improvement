package com.quectel.web.cloud.salesleadhubserver.aspect;

import com.quectel.web.cloud.salesleadhubserver.annotation.AuditAction;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 业务操作审计切面：拦截 {@link AuditAction} 标注的 service 方法，成功/失败各落一行 audit_log。
 *
 * <p><b>为什么必须在切面（请求线程）内取好 operator/ip/ua</b>：真正的落库是异步的
 * （{@link AuditLogWriter#write}），异步线程已丢失 SecurityContext 与 RequestContext，
 * 那时再取会全 null。故本切面在请求线程内组装完整 {@link AuditLogDO} 再交给 writer。</p>
 *
 * <p>before/after 快照本期一律 null——留待下期接入（需在方法执行前后各查一次目标行做 diff，
 * 属独立增强，此处仅留锚点）。写审计失败绝不影响主流程：writer 内部 try/catch 吞掉，
 * 且异步已脱离主事务。</p>
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_FAIL = "fail";
    private static final String ACTION_PUBLISH = "publish";
    private static final String ACTION_ARCHIVE = "archive";

    private final CurrentUserResolver currentUser;
    private final AuditLogWriter writer;

    public AuditLogAspect(CurrentUserResolver currentUser, AuditLogWriter writer) {
        this.currentUser = currentUser;
        this.writer = writer;
    }

    @Around("@annotation(auditAction)")
    public Object around(ProceedingJoinPoint pjp, AuditAction auditAction) throws Throwable {
        // 请求线程内取好操作人 + 环境（异步落库时已取不到）
        Long operatorId = null;
        String operatorName = null;
        try {
            SysUserDO me = currentUser.currentOrNull();
            if (me != null) {
                operatorId = me.getId();
                operatorName = me.getName();
            }
        } catch (RuntimeException ignore) {
            // 取操作人失败不影响主流程，审计留空
        }
        String actionType = resolveActionType(auditAction, pjp.getArgs());
        String target = auditAction.targetType() + "#" + resolveId(pjp.getArgs());
        String ip = resolveIp();
        String ua = resolveUserAgent();

        try {
            Object ret = pjp.proceed();
            safeWrite(operatorId, operatorName, actionType, target, RESULT_SUCCESS, ip, ua);
            return ret;
        } catch (Throwable ex) {
            // 失败路径也留痕，再原样抛出
            safeWrite(operatorId, operatorName, actionType, target, RESULT_FAIL, ip, ua);
            throw ex;
        }
    }

    /** 组装并交异步 writer；组装/派发失败只 warn，绝不影响主流程。 */
    private void safeWrite(Long operatorId, String operatorName, String actionType,
                           String target, String result, String ip, String ua) {
        try {
            AuditLogDO row = new AuditLogDO();
            row.setOperatorId(operatorId);
            row.setOperatorName(operatorName);
            row.setActionType(actionType);
            row.setTarget(target);
            row.setResult(result);
            row.setIpAddress(ip);
            row.setUserAgent(ua);
            row.setBeforeSnapshot(null);   // 快照本期置 null，下期补 diff
            row.setAfterSnapshot(null);
            writer.write(row);
        } catch (RuntimeException e) {
            log.warn("组装/派发审计日志失败(已忽略): actionType={}, target={}", actionType, target, e);
        }
    }

    /** AUTO_STATUS 时从 String 状态入参派生 publish/archive；否则用注解声明值。 */
    private String resolveActionType(AuditAction auditAction, Object[] args) {
        if (!AuditAction.AUTO_STATUS.equals(auditAction.actionType())) {
            return auditAction.actionType();
        }
        for (Object a : args) {
            if (a instanceof String) {
                String s = (String) a;
                if ("published".equals(s)) {
                    return ACTION_PUBLISH;
                }
                if ("archived".equals(s) || "Closed".equals(s)) {
                    return ACTION_ARCHIVE;
                }
            }
        }
        // 派生不出（缺状态入参）默认按下架，宁可保守标 archive 也不落非法枚举
        return ACTION_ARCHIVE;
    }

    /**
     * 从入参提取目标 id：优先首个 Long，其次带 getId() 的 DTO。取不到返回 "-"。
     */
    private String resolveId(Object[] args) {
        if (args == null) {
            return "-";
        }
        for (Object a : args) {
            if (a instanceof Long) {
                return String.valueOf(a);
            }
        }
        for (Object a : args) {
            if (a == null || a instanceof String) {
                continue;
            }
            try {
                Method getId = a.getClass().getMethod("getId");
                Object id = getId.invoke(a);
                if (id != null) {
                    return String.valueOf(id);
                }
            } catch (ReflectiveOperationException ignore) {
                // 无 getId 的入参跳过
            }
        }
        return "-";
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getRequest();
        }
        return null;   // 定时任务/异步等非 web 上下文
    }

    /** IP：X-Forwarded-For 首段优先，兜底 remoteAddr。 */
    private String resolveIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return req.getRemoteAddr();
    }

    private String resolveUserAgent() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader("User-Agent");
    }
}
