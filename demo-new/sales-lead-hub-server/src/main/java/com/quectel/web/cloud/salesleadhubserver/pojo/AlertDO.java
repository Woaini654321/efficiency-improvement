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
 * 运营告警（alert）。轻量派生表：低发布量/低触达/SLA 超时。仅有 create_time + update_time（无 create_by/update_by），故不继承 {@link BaseEntity}。
 *
 * <p>detail 为自由结构 JSON，映射为 String 原样存取。</p>
 */
@Data
@TableName("alert")
public class AlertDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** low_publish/low_reach/sla_breach */
    private String alertType;

    /** pending/resolved */
    private String alertStatus;

    /** 关联对象 id */
    private Long targetId;

    /** 告警详情（自由结构 JSON 文本） */
    private String detail;

    /** 处理人 FK */
    private Long resolvedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
