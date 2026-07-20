package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.SolutionResponseMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.SolutionResponseDO;
import org.springframework.stereotype.Repository;

@Repository
public class SolutionResponseDaoImpl extends ServiceImpl<SolutionResponseMapper, SolutionResponseDO>
        implements SolutionResponseDao {
}
