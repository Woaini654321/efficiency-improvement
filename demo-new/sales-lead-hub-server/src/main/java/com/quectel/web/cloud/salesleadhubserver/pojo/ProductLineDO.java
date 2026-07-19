package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 产品线（product_line）。SLA L1 升级人来源。含全部 4 审计列，继承 {@link BaseEntity}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("product_line")
public class ProductLineDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 产品线名 */
    private String name;

    /** 描述 */
    private String description;

    /** 1启用 0停用 */
    private Integer isActive;

    /** 乐观锁 */
    @Version
    private Integer version;
}
