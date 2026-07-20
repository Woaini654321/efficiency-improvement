package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionPostDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.DiscussionPostMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import org.springframework.stereotype.Repository;

@Repository
public class DiscussionPostDaoImpl extends ServiceImpl<DiscussionPostMapper, DiscussionPostDO>
        implements DiscussionPostDao {

    @Override
    public boolean increaseViewCount(Long id) {
        // WHERE id=? SET view_count = view_count + 1，单条原子自增
        return this.lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(DiscussionPostDO::getId, id)
                .update();
    }

    @Override
    public boolean increaseReplyCount(Long id) {
        // WHERE id=? SET reply_count = reply_count + 1，单条原子自增
        return this.lambdaUpdate()
                .setSql("reply_count = reply_count + 1")
                .eq(DiscussionPostDO::getId, id)
                .update();
    }
}
