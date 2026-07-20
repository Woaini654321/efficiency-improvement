package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.SubscriptionDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.SubscriptionMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.SubscriptionDO;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionDaoImpl extends ServiceImpl<SubscriptionMapper, SubscriptionDO>
        implements SubscriptionDao {
}
