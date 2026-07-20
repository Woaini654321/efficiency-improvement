package com.quectel.web.cloud.salesleadhubserver.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「需要落业务操作日志（audit_log）」的 service 方法。由 {@code AuditLogAspect} 环绕拦截。
 *
 * <p>{@link #actionType()} 必须取 audit_log schema 注释里的枚举之一：
 * publish/archive/delete/role_change/isolation_change/category_change/login/sla_escalation，
 * 不得造新值。若某方法的 action_type 需按运行时目标态在 publish/archive 间切换，
 * 用特殊值 {@link #AUTO_STATUS}，切面会从方法的 String 状态入参派生。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {

    /**
     * 特殊 actionType：由切面从方法的状态入参派生 publish/archive。
     *
     * <p>派生规则：入参里出现 {@code published} → publish；{@code archived}/{@code Closed} → archive。
     * 用于 changeStatus 这类「上/下架同一个方法、动作随目标态变」的场景。</p>
     */
    String AUTO_STATUS = "__auto_status__";

    /** 操作类型，取 audit_log 枚举之一，或 {@link #AUTO_STATUS}。 */
    String actionType();

    /** 操作对象类型描述（如 Opportunity/Request/Announcement/Category/Content），拼进 target。 */
    String targetType();
}
