package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 方案响应（solution_response）。需求-方案匹配核心。含全部 4 审计列，继承 {@link BaseEntity}。
 *
 * <p>含 List JSON 字段与乐观锁 version，故 {@code autoResultMap = true}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "solution_response", autoResultMap = true)
public class SolutionResponseDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属需求 FK */
    private Long requestId;

    /** 富文本方案 */
    private String content;

    /** 附件 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachments;

    /** 响应人 FK */
    private Long responderId;

    /** 响应人姓名快照 */
    private String responderName;

    /** 响应人部门 FK */
    private Long departmentId;

    /** 响应部门名快照 */
    private String responderDeptName;

    /** 派生/冗余，与 request.adopted_response_id 同事务更新 */
    private Integer isAdopted;

    /** 邮件通知人 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> emailRecipients;

    /** 自定义邮件通知人 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> customEmailRecipients;

    /** 是否同步飞书 */
    private Integer feishuSync;

    /** 乐观锁 */
    @Version
    private Integer version;
}
