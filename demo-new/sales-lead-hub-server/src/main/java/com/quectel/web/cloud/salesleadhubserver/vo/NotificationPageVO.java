package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知列表项出参。类级 @JsonNaming 出 snake_case（禁全局策略，理由见 OpportunityPageVO）。
 *
 * <p>前端 adapter 读的键名是 {@code notification_id}（不是 id）、{@code is_read}、
 * {@code trigger_user_name} 等；is_read/is_force_confirm 契约是 boolean，convert 负责由
 * TINYINT(Integer) 转换。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class NotificationPageVO {

    /** 前端 adapter 读的键名是 notification_id */
    private Long notificationId;

    /** publish/response/adopt/system/mention/subscribe/force_confirm */
    private String type;

    /** in_app/feishu/email */
    private String channel;

    private String title;

    private String triggerUserName;

    /** DB 是 TINYINT(1)，契约是 boolean */
    private Boolean isRead;

    /** DB 是 TINYINT(1)，契约是 boolean */
    private Boolean isForceConfirm;

    /** opportunity/requirement/announcement/... */
    private String targetType;

    /** 前端 ID 一律 string，Long 由全局 Jackson 序列化成 string */
    private Long targetId;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
