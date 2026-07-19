package com.quectel.web.cloud.salesleadhubserver.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 覆盖框架 mysql-starter 的 {@link MybatisPlusInterceptor}，以启用乐观锁。
 *
 * <p><b>为何允许覆盖（本项目 CLAUDE.md §3/§11 原则上禁定义 MyBatisPlusConfig）</b>：
 * 反编译 {@code quectel-code-mysql-starter} 的 {@code MyBatisPlusConfig.mybatisPlusInterceptor()}
 * 确认其<b>只注册了 {@link PaginationInnerInterceptor}</b>（MYSQL，maxLimit=500），
 * <b>未注册 {@link OptimisticLockerInnerInterceptor}</b> —— 导致实体上的 {@code @Version}
 * 完全失效：{@code updateById} 既不生成 {@code WHERE version=?}，也不做 {@code version+1}，
 * 并发更新会静默后写覆盖先写。</p>
 *
 * <p>该框架 {@code @Bean} 带 {@code @ConditionalOnMissingBean}，<b>设计上明示允许业务侧替换</b>。
 * 商机需求存在多人并发编辑场景，必须有乐观锁保护，故经用户明确批准覆盖。</p>
 *
 * <p><b>强制约束</b>：必须逐字复刻框架原有的分页配置（MYSQL + maxLimit=500），
 * 否则覆盖会连带丢失分页保护。且分页拦截器<b>必须放在最后</b>（MyBatis-Plus 官方要求：
 * 分页拦截器之后不应再有其它 inner interceptor）。</p>
 */
@Configuration
public class MybatisPlusOptimisticLockConfig {

    /** 框架原配置的分页上限，复刻自 mysql-starter 字节码常量，勿改。 */
    private static final long PAGE_MAX_LIMIT = 500L;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1) 乐观锁（本次新增）—— 必须在分页之前注册
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 2) 分页 —— 逐字复刻框架原配置，勿改
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(PAGE_MAX_LIMIT);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}
