package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.CompetitorIntelDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.CompetitorIntelMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.CompetitorIntelDO;
import org.springframework.stereotype.Repository;

@Repository
public class CompetitorIntelDaoImpl extends ServiceImpl<CompetitorIntelMapper, CompetitorIntelDO>
        implements CompetitorIntelDao {

    @Override
    public boolean increaseViewCount(Long id) {
        return lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(CompetitorIntelDO::getId, id)
                .update();
    }
}
