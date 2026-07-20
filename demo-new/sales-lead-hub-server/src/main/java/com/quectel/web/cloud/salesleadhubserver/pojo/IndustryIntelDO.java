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
 * 行业情报（industry_intel）。含全部 4 审计列，继承 {@link BaseEntity}。
 *
 * <p>含 List JSON 字段 keyPoints，故 {@code autoResultMap = true}。无逻辑删除、无乐观锁。
 * 行业情报无提交入口（本期只读 + 运营下期维护），故无 submitter 字段。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "industry_intel", autoResultMap = true)
public class IndustryIntelDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** trend/automotive/policy/energy/industrial/smartcity */
    private String industry;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 来源 */
    private String source;

    /** 详情-概述 */
    private String overview;

    /** 详情-分析 */
    private String analysis;

    /** 详情-影响 */
    private String impact;

    /** 要点列表（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> keyPoints;

    /** 点赞数 */
    private Integer likeCount;

    /** 收藏数 */
    private Integer collectCount;

    /** 浏览数 */
    private Integer viewCount;
}
