package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.AnnouncementDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.AnnouncementMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementDaoImpl extends ServiceImpl<AnnouncementMapper, AnnouncementDO>
        implements AnnouncementDao {
}
