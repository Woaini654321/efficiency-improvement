package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 匿名反馈（吐槽墙，feedback）。单表，含全部 4 审计列，继承 {@link BaseEntity}。
 *
 * <p>无逻辑删除、无乐观锁（吐槽墙无编辑/删除交互）。createBy 存真实作者仅作后台反滥用留痕，
 * <b>任何 VO 都不得输出 create_by</b>——匿名是本模块的产品语义红线。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("feedback")
public class FeedbackDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 匿名昵称（后端生成，不暴露真实身份） */
    private String anonName;

    /** 表情 */
    private String emoji;

    /** 卡片色 */
    private String color;

    /** 点赞数（无去重，简单原子自增） */
    private Integer likeCount;
}
