package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.AuditLogDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.AuditLogMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogDaoImpl extends ServiceImpl<AuditLogMapper, AuditLogDO>
        implements AuditLogDao {
}
