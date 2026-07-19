package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 互动记录（interaction）。评论(≤2级) / 收藏 / 点赞统一单表。仅有 create_time。
 *
 * <p>注意：本表<b>无</b>框架 deleted 逻辑删除字段；contentDeleted 是评论 D7 软删的<b>普通业务字段</b>（非 {@code @TableLogic}）。
 * like/collect 取消走物理删（removeById）。</p>
 * <p>reactionUk 是 DB 生成列（GENERATED ALWAYS ... STORED），仅供 select 读取，insert/update 一律不写（策略 NEVER）。</p>
 */
@Data
@TableName("interaction")
public class InteractionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 用户 FK */
    private Long userId;

    /** 用户姓名快照(评论展示) */
    private String userName;

    /** Opportunity/Request/Response */
    private String targetType;

    /** 目标 id */
    private Long targetId;

    /** comment/collect/like */
    private String type;

    /** 仅 comment 有值 */
    private String content;

    /** ≤2级：NULL=一级评论，非NULL须指向一级评论 */
    private Long parentCommentId;

    /** D7 评论软删占位（行保留/内容转占位/留子回复），普通字段，非框架 deleted */
    private Integer contentDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** like/collect 去重键，DB 生成列；仅读，禁写入 */
    @TableField(value = "reaction_uk", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private String reactionUk;
}
