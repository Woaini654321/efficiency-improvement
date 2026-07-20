package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.OpportunityCategoryMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityCategoryDO;
import org.springframework.stereotype.Repository;

@Repository
public class OpportunityCategoryDaoImpl extends ServiceImpl<OpportunityCategoryMapper, OpportunityCategoryDO>
        implements OpportunityCategoryDao {
}
