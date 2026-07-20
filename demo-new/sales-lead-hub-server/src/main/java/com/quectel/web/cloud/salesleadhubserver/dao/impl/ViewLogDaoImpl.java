package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.ViewLogDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.ViewLogMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;
import org.springframework.stereotype.Repository;

@Repository
public class ViewLogDaoImpl extends ServiceImpl<ViewLogMapper, ViewLogDO>
        implements ViewLogDao {
}
