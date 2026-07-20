package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.MeetingTaskMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import org.springframework.stereotype.Repository;

@Repository
public class MeetingTaskDaoImpl extends ServiceImpl<MeetingTaskMapper, MeetingTaskDO>
        implements MeetingTaskDao {
}
