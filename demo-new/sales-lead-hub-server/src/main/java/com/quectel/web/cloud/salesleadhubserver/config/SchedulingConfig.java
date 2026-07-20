package com.quectel.web.cloud.salesleadhubserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务与异步执行。
 *
 * <ul>
 *   <li>{@code @EnableScheduling}：驱动 {@code SlaScheduler} 的 {@code @Scheduled}（SLA 升级 + 7 天自动关闭）。</li>
 *   <li>{@code @EnableAsync}：驱动 {@code AuditLogWriter} 的 {@code @Async}（审计行旁路异步落库）。</li>
 * </ul>
 *
 * <p>未自定义线程池，用 Spring 默认执行器即可：审计与调度均为低频、低并发的旁路任务。</p>
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
