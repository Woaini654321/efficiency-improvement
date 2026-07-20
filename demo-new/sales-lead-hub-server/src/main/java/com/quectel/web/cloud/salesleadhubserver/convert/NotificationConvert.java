package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.vo.NotificationPageVO;
import org.springframework.stereotype.Component;

/**
 * 手写 DO → VO 映射。TINYINT(Integer) ↔ boolean（契约）转换的唯一桥接点。
 *
 * <p>无 DTO→DO 映射：本模块不负责通知的产生（发通知由各业务动作后续接入），
 * 只做查询与已读，故没有 create/applyUpdate。</p>
 */
@Component
public class NotificationConvert {

    public NotificationPageVO toPageVO(NotificationDO d) {
        NotificationPageVO v = new NotificationPageVO();
        v.setNotificationId(d.getId());
        v.setType(d.getType());
        v.setChannel(d.getChannel());
        v.setTitle(d.getTitle());
        v.setTriggerUserName(d.getTriggerUserName());
        v.setIsRead(toBool(d.getIsRead()));
        v.setIsForceConfirm(toBool(d.getIsForceConfirm()));
        v.setTargetType(d.getTargetType());
        v.setTargetId(d.getTargetId());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    /** TINYINT(1) → boolean：非 null 且 ==1 为 true，其余 false。 */
    private Boolean toBool(Integer v) {
        return v != null && v == 1;
    }
}
