package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;

/** 公告主表 DAO（单表，无关联表；无逻辑删除列 deleted，removeById 即物理删）。 */
public interface AnnouncementDao extends IService<AnnouncementDO> {
}
