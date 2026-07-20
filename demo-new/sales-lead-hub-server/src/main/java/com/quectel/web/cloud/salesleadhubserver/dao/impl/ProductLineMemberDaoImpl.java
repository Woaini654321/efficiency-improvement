package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.ProductLineMemberMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import org.springframework.stereotype.Repository;

@Repository
public class ProductLineMemberDaoImpl extends ServiceImpl<ProductLineMemberMapper, ProductLineMemberDO>
        implements ProductLineMemberDao {
}
