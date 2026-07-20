package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestProductLineDO;

/**
 * 需求-邀请产品线关联（request_product_line）数据访问。
 *
 * <p>SLA L1 升级人（产品线负责人）解析走本表：需求 → 邀请产品线 → product_line_member(is_owner=1)。
 * 只读消费。</p>
 *
 * <p><b>⚠️ 复合主键表</b>：{@code requestId} 标 {@code IdType.INPUT} 仅为让 insert 工作，
 * 关系查询走 {@code LambdaQueryWrapper}，禁 getById/removeById（语义只按单列匹配）。</p>
 */
public interface RequestProductLineDao extends IService<RequestProductLineDO> {
}
