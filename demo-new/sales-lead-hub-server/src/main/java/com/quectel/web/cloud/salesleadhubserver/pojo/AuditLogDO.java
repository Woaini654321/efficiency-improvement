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
 * 操作日志（audit_log）。只读追加，不可篡改。仅有 create_time。
 *
 * <p>before_snapshot / after_snapshot 为自由结构 JSON，映射为 String 原样存取。</p>
 */
@Data
@TableName("audit_log")
public class AuditLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 操作人 FK */
    private Long operatorId;

    /** 操作人姓名快照 */
    private String operatorName;

    /** publish/archive/delete/role_change/isolation_change/category_change/login/sla_escalation */
    private String actionType;

    /** 操作对象描述 */
    private String target;

    /** success/fail */
    private String result;

    /** IP(对外脱敏见§6) */
    private String ipAddress;

    /** UA */
    private String userAgent;

    /** 变更前快照（自由结构 JSON 文本） */
    private String beforeSnapshot;

    /** 变更后快照（自由结构 JSON 文本） */
    private String afterSnapshot;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
