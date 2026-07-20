package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;

/** 通知主表 DAO。仅查询与已读维护，通知的产生由各业务动作后续接入（见 NotificationService javadoc）。 */
public interface NotificationDao extends IService<NotificationDO> {
}
