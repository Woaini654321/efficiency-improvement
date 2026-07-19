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
 * 订阅（subscription）。独立实体，替代 User.subscriptions JSON（D9）。仅有 create_time。
 */
@Data
@TableName("subscription")
public class SubscriptionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 订阅用户 FK */
    private Long userId;

    /** 订阅分类 FK */
    private Long categoryId;

    /** 分类名快照 */
    private String categoryName;

    /** default_dept/manual */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
