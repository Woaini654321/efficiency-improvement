package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.FeedbackDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.FeedbackMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.FeedbackDO;
import org.springframework.stereotype.Repository;

@Repository
public class FeedbackDaoImpl extends ServiceImpl<FeedbackMapper, FeedbackDO>
        implements FeedbackDao {

    @Override
    public boolean increaseLikeCount(Long id) {
        return lambdaUpdate()
                .setSql("like_count = like_count + 1")
                .eq(FeedbackDO::getId, id)
                .update();
    }
}
