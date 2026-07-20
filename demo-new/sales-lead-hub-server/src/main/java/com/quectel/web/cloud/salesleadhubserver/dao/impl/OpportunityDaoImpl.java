package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.OpportunityMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import org.springframework.stereotype.Repository;

@Repository
public class OpportunityDaoImpl extends ServiceImpl<OpportunityMapper, OpportunityDO>
        implements OpportunityDao {
}
