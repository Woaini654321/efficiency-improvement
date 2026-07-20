package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;

/** 需求（opportunity_request）数据访问。backing mapper 为已有的 OpportunityRequestMapper。 */
public interface RequirementDao extends IService<OpportunityRequestDO> {

    /**
     * 收到方案时原子推进：response_count + 1，且若仍是 Pending 则置 Collecting。
     *
     * <p>用一条 {@code UPDATE ... SET c = c + 1} 原子自增，不读出再写回——计数列无乐观锁保护，
     * 并发下读改写会丢计数。状态用 {@code CASE WHEN status='Pending'} 派生：首响停表由
     * {@code response_count>0} 决定（{@code SlaCalculator.isResponded}），这里只做首响时把
     * 展示态从「待响应」推进到「收集中」，Collecting/Adopted/Closed 不回退。</p>
     *
     * @return 是否命中并更新（影响行数>0）
     */
    default boolean increaseResponseCount(Long requestId) {
        return lambdaUpdate()
                .setSql("response_count = response_count + 1, "
                        + "status = CASE WHEN status = 'Pending' THEN 'Collecting' ELSE status END")
                .eq(OpportunityRequestDO::getId, requestId)
                .update();
    }
}
