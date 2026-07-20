package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionReplyDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.DiscussionReplyMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionReplyDO;
import org.springframework.stereotype.Repository;

@Repository
public class DiscussionReplyDaoImpl extends ServiceImpl<DiscussionReplyMapper, DiscussionReplyDO>
        implements DiscussionReplyDao {
}
