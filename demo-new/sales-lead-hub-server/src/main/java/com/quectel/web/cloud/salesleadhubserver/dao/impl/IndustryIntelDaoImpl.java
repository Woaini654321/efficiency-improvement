package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.IndustryIntelDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.IndustryIntelMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.IndustryIntelDO;
import org.springframework.stereotype.Repository;

@Repository
public class IndustryIntelDaoImpl extends ServiceImpl<IndustryIntelMapper, IndustryIntelDO>
        implements IndustryIntelDao {

    @Override
    public boolean increaseViewCount(Long id) {
        return lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(IndustryIntelDO::getId, id)
                .update();
    }
}
