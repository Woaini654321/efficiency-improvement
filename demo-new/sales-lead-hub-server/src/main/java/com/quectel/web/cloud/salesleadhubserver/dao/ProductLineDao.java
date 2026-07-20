package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineDO;

import java.util.List;

/** 产品线主表 DAO。 */
public interface ProductLineDao extends IService<ProductLineDO> {

    /**
     * 查启用中的产品线（{@code is_active=1}），按 name 升序。供产品线选项选择器使用。
     *
     * <p>把 is_active 过滤 + 排序封装成 Dao 方法（而非在 service 裸写 lambdaQuery），
     * 与 {@code DiscussionPostDao.increaseViewCount} 同理：便于 service 单测直接 mock，
     * 无需 mock MyBatis-Plus 的链式调用。</p>
     */
    List<ProductLineDO> listActiveOrderByName();
}
