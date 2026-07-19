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
 * 通知批次（notification_batch）。支撑按批次已读率/触达率。仅有 create_time。
 */
@Data
@TableName("notification_batch")
public class NotificationBatchDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次类型(同 notification.type) */
    private String type;

    /** 触发源对象 id */
    private Long sourceTargetId;

    /** 推送总数 */
    private Integer totalCount;

    /** 已读数 */
    private Integer readCount;

    /** 已确认数 */
    private Integer confirmCount;

    /** 推送时间 */
    private LocalDateTime pushedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
