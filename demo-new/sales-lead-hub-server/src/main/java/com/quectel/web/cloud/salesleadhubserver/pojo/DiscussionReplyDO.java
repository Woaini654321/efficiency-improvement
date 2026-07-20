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
 * 讨论回帖（discussion_reply）。自引用树：{@code parentId} 为空=一级回帖，否则指向父回帖。
 *
 * <p><b>刻意不继承 {@link BaseEntity}</b>：discussion_reply 表只有 create_time 一个时间列，
 * 没有 create_by/update_by/update_time。若继承 BaseEntity，框架
 * {@code SecurityMetaObjectHandler} 会尝试写这三个不存在的列导致 SQL 异常。这里只声明
 * createTime 并用 INSERT 填充策略（自身有单主键 id，非复合主键，故 Dao 可正常
 * getById/save，与 opportunity_category 关联表不同）。</p>
 *
 * <p>回帖深度不限——讨论区属下期模块，<b>不受 MOD-04「评论 ≤ 2 级」约束</b>
 * （那条约束只作用于 interaction 模块的商机/需求页评论）。</p>
 */
@Data
@TableName("discussion_reply")
public class DiscussionReplyDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属帖 FK */
    private Long postId;

    /** 父回帖 FK（NULL=一级回帖，自引用树） */
    private Long parentId;

    /** 作者 FK */
    private Long authorId;

    /** 作者姓名快照 */
    private String authorName;

    /** 内容 */
    private String content;

    /** 创建时间（表仅此一个时间列，INSERT 时由框架填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
