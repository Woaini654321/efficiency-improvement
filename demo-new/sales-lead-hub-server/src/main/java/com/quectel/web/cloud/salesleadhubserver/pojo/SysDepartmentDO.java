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
 * 部门（sys_department）。自引用部门树。仅有 create_time + update_time（无 create_by/update_by），故不继承 {@link BaseEntity}。
 */
@Data
@TableName("sys_department")
public class SysDepartmentDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 部门名 */
    private String name;

    /** 父部门 FK(自引用)，NULL=根 */
    private Long parentId;

    /** 部门负责人 FK → sys_user.id */
    private Long ownerId;

    /** 负责人姓名快照 */
    private String ownerName;

    /** 同级排序 */
    private Integer sortOrder;

    /** 1启用 0停用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
