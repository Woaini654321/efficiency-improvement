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
 * 商机信息（opportunity）。核心表，FSM OPP。含全部 4 审计列，继承 {@link BaseEntity}。
 *
 * <p>含逻辑删除字段 deleted（{@link TableLogic}）与乐观锁 version；含 List JSON 字段，故 {@code autoResultMap = true}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "opportunity", autoResultMap = true)
public class OpportunityDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 富文本正文 */
    private String content;

    /** product_info/solution/success_case */
    private String type;

    /** 附件[{name,url,size}]（JSON 数组，暂以 List<String> 占位承载） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachments;

    /** draft/published/archived */
    private String status;

    /** 发布人 FK */
    private Long publisherId;

    /** 发布人姓名快照 */
    private String publisherName;

    /** 发布部门 FK */
    private Long departmentId;

    /** 发布部门名快照 */
    private String publisherDeptName;

    /** 下架人 FK(谁下架谁恢复) */
    private Long archivedBy;

    /** 分类名快照(展示免JOIN，关系见 opportunity_category) */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> categoryNames;

    /** 浏览数(去重后自增) */
    private Integer viewCount;

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
