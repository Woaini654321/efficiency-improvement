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
 * 分类标签（category）。自引用树。仅有 create_time + update_time（无 create_by/update_by），故不继承 {@link BaseEntity}。
 */
@Data
@TableName("category")
public class CategoryDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分类名(权威中文名) */
    private String name;

    /** 英文名(空则回退中文) */
    private String nameEn;

    /** 父分类 FK(自引用)，NULL=根 */
    private Long parentId;

    /** 同级排序 */
    private Integer sortOrder;

    /** 1启用 0停用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
