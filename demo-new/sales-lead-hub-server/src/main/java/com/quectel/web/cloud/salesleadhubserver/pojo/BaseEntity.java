package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计基类：封装 createBy / updateBy / createTime / updateTime 四个审计字段。
 *
 * <p>这四个字段由 security-starter 的 {@code SecurityMetaObjectHandler} 在 insert / update 时<b>自动填充</b>：
 * insert 填 createTime/updateTime/createBy/updateBy，update 填 updateTime/updateBy。业务代码<b>禁止</b>手动赋值。</p>
 *
 * <p>createBy/updateBy 存当前用户 id（{@code getCurrentUserId()} 返回 Long）；createTime/updateTime 为 {@link LocalDateTime}。</p>
 *
 * <p>仅含全部 4 个审计列的实体继承本类：sys_user、product_line、opportunity、opportunity_request、solution_response、announcement。</p>
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 创建人 id（当前用户 Long id），insert 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 更新人 id（当前用户 Long id），insert 与 update 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /** 创建时间，insert 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，insert 与 update 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
