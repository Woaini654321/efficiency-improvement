package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 商机需求（opportunity_request）。核心表，FSM REQ，含 SLA 派生字段。含全部 4 审计列，继承 {@link BaseEntity}。
 *
 * <p>含逻辑删除字段 deleted（{@link TableLogic}）与乐观锁 version；含多个 List JSON 字段，故 {@code autoResultMap = true}。</p>
 * <p>注：priority/slaRemaining 由 urgency + createTime 派生，不入库；deadline 已移除。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "opportunity_request", autoResultMap = true)
public class OpportunityRequestDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标题 */
    private String title;

    /** 需求描述 */
    private String description;

    /** 行业 */
    private String industry;

    /** normal/urgent/critical(存储) */
    private String urgency;

    /** Pending/Collecting/Adopted/Closed */
    private String status;

    /** 采纳方案 FK(采纳单一真相源) */
    private Long adoptedResponseId;

    /** 发布人 FK */
    private Long publisherId;

    /** 发布人姓名快照 */
    private String publisherName;

    /** 发布部门 FK */
    private Long departmentId;

    /** 发布部门名快照 */
    private String publisherDeptName;

    /** all/dept/personnel(收窄 D4) */
    private String visibilityScope;

    /** dept/personnel 时的具体范围 id 集 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> visibilityValues;

    /** 邀请产品线 id 集 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> invitedProductLineIds;

    /** 邀请产品线名快照(展示免JOIN) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> invitedProductLineNames;

    /** 分类名快照(关系见 request_category) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> categoryNames;

    /** normal/warning/overdue/responded(派生) */
    private String slaStatus;

    /** L0/L1/L2/L3 */
    private String escalationLevel;

    /** 浏览数 */
    private Integer viewCount;

    /** 方案响应数 */
    private Integer responseCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 收藏数 */
    private Integer collectCount;

    /** 评论数 */
    private Integer commentCount;

    /** 逻辑删除 Y删/N正常（框架管理，勿手动赋值） */
    @TableLogic(value = "N", delval = "Y")
    private String deleted;

    /** 乐观锁 */
    @Version
    private Integer version;
}
