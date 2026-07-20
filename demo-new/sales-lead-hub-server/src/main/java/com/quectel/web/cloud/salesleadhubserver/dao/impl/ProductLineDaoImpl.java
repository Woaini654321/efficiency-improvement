package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.ProductLineMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineDO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductLineDaoImpl extends ServiceImpl<ProductLineMapper, ProductLineDO>
        implements ProductLineDao {

    @Override
    public List<ProductLineDO> listActiveOrderByName() {
        // WHERE is_active = 1 ORDER BY name ASC
        return this.lambdaQuery()
                .eq(ProductLineDO::getIsActive, 1)
                .orderByAsc(ProductLineDO::getName)
                .list();
    }
}
