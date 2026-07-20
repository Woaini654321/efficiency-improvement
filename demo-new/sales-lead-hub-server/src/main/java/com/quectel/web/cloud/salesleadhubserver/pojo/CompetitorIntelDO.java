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
 * 竞品情报（competitor_intel）。含全部 4 审计列，继承 {@link BaseEntity}。
 *
 * <p>含 List JSON 字段 specs，故 {@code autoResultMap = true}。无逻辑删除、无乐观锁。
 * 计数列 like/collect/view 由查询时原子自增维护，非乐观锁保护。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "competitor_intel", autoResultMap = true)
public class CompetitorIntelDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 竞品品牌 */
    private String brand;

    /** 竞品产品 */
    private String product;

    /** new_product/price_change/customer_case/other */
    private String intelType;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 情报来源 */
    private String source;

    /** 提交人 FK */
    private Long submitterId;

    /** 提交人姓名快照 */
    private String submitterName;

    /** 详情-概述（submit 正文落此列） */
    private String overview;

    /** 详情-分析 */
    private String analysis;

    /** 详情-影响 */
    private String impact;

    /** 关键信息[{label,value}]（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<SpecItem> specs;

    /** 点赞数 */
    private Integer likeCount;

    /** 收藏数 */
    private Integer collectCount;

    /** 浏览数 */
    private Integer viewCount;
}
