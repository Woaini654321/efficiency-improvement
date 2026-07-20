package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCenterVO;

/**
 * 个人中心聚合业务。
 *
 * <p>只返回<b>当前登录人自己</b>的数据，不接受 userId 入参（防越权查他人）。</p>
 */
public interface ProfileService {

    /** 聚合当前登录人的 {user, stats, subscription_tree(+subscribed_keys)}。 */
    ProfileCenterVO center();
}
