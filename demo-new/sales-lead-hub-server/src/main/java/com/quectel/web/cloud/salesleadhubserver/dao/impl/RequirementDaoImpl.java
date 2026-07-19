package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.OpportunityRequestMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import org.springframework.stereotype.Repository;

@Repository
public class RequirementDaoImpl extends ServiceImpl<OpportunityRequestMapper, OpportunityRequestDO>
        implements RequirementDao {
}
