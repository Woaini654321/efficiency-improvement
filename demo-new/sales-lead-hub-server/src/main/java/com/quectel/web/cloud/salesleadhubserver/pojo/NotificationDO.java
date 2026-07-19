package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知（notification）。FSM NOTIF + 强制确认。仅有 create_time。
 */
@Data
@TableName("notification")
public class NotificationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收人 FK */
    private Long userId;

    /** 所属推送批次 FK */
    private Long batchId;

    /** 关联对象类型 */
    private String targetType;

    /** 关联对象 id */
    private Long targetId;

    /** publish/response/adopt/system/comment/reply/invite/force_confirm/sla_remind/sla_escalate/archive/category_change/announcement */
    private String type;

    /** in_app/feishu/email */
    private String channel;

    /** 通知标题快照 */
    private String title;

    /** 触发人姓名快照 */
    private String triggerUserName;

    /** 是否已读 */
    private Integer isRead;

    /** 是否强制确认阅读 */
    private Integer isForceConfirm;

    /** 强制确认时间 */
    private LocalDateTime confirmTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
