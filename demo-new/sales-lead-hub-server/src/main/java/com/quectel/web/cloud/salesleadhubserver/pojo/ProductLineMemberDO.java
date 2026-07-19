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
 * 产品线成员（product_line_member）。产品线↔用户 + 负责人标记(L1 升级人)。仅有 create_time。
 */
@Data
@TableName("product_line_member")
public class ProductLineMemberDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 产品线 FK */
    private Long productLineId;

    /** 用户 FK */
    private Long userId;

    /** 成员姓名快照 */
    private String userName;

    /** 1=产品线负责人(L1升级人) */
    private Integer isOwner;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
