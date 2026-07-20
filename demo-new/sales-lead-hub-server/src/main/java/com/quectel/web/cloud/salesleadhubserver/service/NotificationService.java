package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPreferenceDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.NotificationPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/**
 * 通知与订阅偏好业务。
 *
 * <p><b>本模块只做查询与已读，不负责通知的「产生」</b>——发通知（新方案响应、采纳、@提及、
 * 订阅推送、强制确认等）由各业务动作在其自身事务里写 notification 行，后续接入，不在此实现。</p>
 *
 * <p>归属硬约束：page/markRead/markAllRead 一律以当前登录人 id 为边界，不接受 userId 入参。</p>
 */
public interface NotificationService {

    /** 分页查询「我的」通知，强制 WHERE user_id = 当前登录人 id。 */
    PageVO<NotificationPageVO> page(NotificationPageDTO dto);

    /** 标记单条已读，须校验该通知归属当前登录人，否则抛 FORBIDDEN。 */
    void markRead(Long id);

    /** 把当前登录人的全部未读一次性置为已读。 */
    void markAllRead();

    /** 保存通知偏好矩阵（类型×渠道开关），整体作为 JSON 落 sys_user.notification_preferences。 */
    void savePreference(NotificationPreferenceDTO dto);
}
