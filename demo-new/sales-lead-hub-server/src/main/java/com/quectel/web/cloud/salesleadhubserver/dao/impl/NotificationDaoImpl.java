package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.NotificationMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationDaoImpl extends ServiceImpl<NotificationMapper, NotificationDO>
        implements NotificationDao {
}
