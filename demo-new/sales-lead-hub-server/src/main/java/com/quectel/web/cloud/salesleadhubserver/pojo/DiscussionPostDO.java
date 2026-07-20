package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * 讨论帖（discussion_post）。讨论区模块主表，含全部 4 审计列，继承 {@link BaseEntity}。
 *
 * <p>{@code tags} 是 JSON 列 → {@code List<String>}，故用 {@link JacksonTypeHandler} +
 * 类级 {@code autoResultMap = true}（写法照抄 {@link OpportunityDO} 的 categoryNames）。</p>
 *
 * <p>与 opportunity 不同：discussion_post 表<b>没有</b> deleted 逻辑删除列，也<b>没有</b>
 * version 乐观锁列（讨论帖属下期模块，暂不做软删/乐观锁），故本 DO 不声明
 * {@code @TableLogic} 与 {@code @Version}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "discussion_post", autoResultMap = true)
public class DiscussionPostDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标题 */
    private String title;

    /** 正文（MEDIUMTEXT） */
    private String content;

    /** business/solution/experience/industry/complaint */
    private String topic;

    /** 作者 FK */
    private Long authorId;

    /** 作者姓名快照（展示免 JOIN） */
    private String authorName;

    /** 标签列表（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    /** 回帖数（运营/展示计数，非实时派生） */
    private Integer replyCount;

    /** 浏览数（详情原子自增） */
    private Integer viewCount;

    /** 热帖标记（TINYINT(1) → Boolean） */
    private Boolean isHot;
}
