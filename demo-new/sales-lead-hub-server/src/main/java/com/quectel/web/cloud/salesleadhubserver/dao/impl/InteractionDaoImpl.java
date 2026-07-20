package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.InteractionDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.InteractionMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import org.springframework.stereotype.Repository;

@Repository
public class InteractionDaoImpl extends ServiceImpl<InteractionMapper, InteractionDO>
        implements InteractionDao {
}
