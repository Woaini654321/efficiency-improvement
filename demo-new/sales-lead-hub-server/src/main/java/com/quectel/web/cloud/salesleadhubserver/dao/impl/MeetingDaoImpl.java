package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.MeetingMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingDO;
import org.springframework.stereotype.Repository;

@Repository
public class MeetingDaoImpl extends ServiceImpl<MeetingMapper, MeetingDO>
        implements MeetingDao {
}
