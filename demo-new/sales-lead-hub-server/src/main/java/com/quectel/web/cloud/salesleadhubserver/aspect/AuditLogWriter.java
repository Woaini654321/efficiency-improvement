package com.quectel.web.cloud.salesleadhubserver.aspect;

import com.quectel.web.cloud.salesleadhubserver.dao.AuditLogDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计行异步落库器。独立成 bean 是为了让 {@link Async} 代理生效
 * （同类内自调用不会走代理，异步会失效）。
 *
 * <p>写失败只 {@code log.warn} 不抛出——审计是旁路，绝不能影响（也已脱离）主业务事务。
 * 操作人/IP/UA 等需请求上下文的字段必须由切面在请求线程内取好后传入，
 * 这里只负责持久化（异步线程已无 RequestContext / SecurityContext）。</p>
 */
@Component
public class AuditLogWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogWriter.class);

    private final AuditLogDao auditLogDao;

    public AuditLogWriter(AuditLogDao auditLogDao) {
        this.auditLogDao = auditLogDao;
    }

    @Async
    public void write(AuditLogDO row) {
        try {
            auditLogDao.save(row);
        } catch (RuntimeException e) {
            log.warn("审计日志落库失败(已忽略，不影响主流程): actionType={}, target={}",
                    row == null ? null : row.getActionType(),
                    row == null ? null : row.getTarget(), e);
        }
    }
}
