package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.SubscriptionDO;

/**
 * 订阅表 DAO。当前由 profile 个人中心只读消费（组订阅树 + 已选 keys）。
 *
 * <p>订阅的写入（全量覆盖：按 user_id 删旧 + saveBatch 新）暂无对应前端端点——
 * 通知偏好页保存的是「类型×渠道」矩阵（落 sys_user.notification_preferences），
 * 与本表无关；个人中心订阅树的保存前端目前仅本地提示未落库。故本 DAO 暂只读。</p>
 */
public interface SubscriptionDao extends IService<SubscriptionDO> {
}
