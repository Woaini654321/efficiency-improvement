package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 用户（sys_user）。id = UAA 用户 id（Long）。含全部 4 审计列，继承 {@link BaseEntity}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("sys_user")
public class SysUserDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录名(UAA) */
    private String username;

    /** 姓名(UAA) */
    private String name;

    /** 工号(本地维护，可选) */
    private String employeeId;

    /** sales/product_manager/admin，单人单角色 */
    private String role;

    /** 部门 FK → sys_department.id */
    private Long departmentId;

    /** 部门名快照 */
    private String departmentName;

    /** active/disabled */
    private String status;

    /** 手机(脱敏见§6) */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像 URL */
    private String avatar;

    /** 通知渠道偏好（自由结构 JSON 文本，原样存取） */
    private String notificationPreferences;

    /** 擅长领域标签（自由结构 JSON 文本，原样存取） */
    private String expertTags;

    /** zh-CN/en-US */
    private String language;

    /** 乐观锁 */
    @Version
    private Integer version;
}
