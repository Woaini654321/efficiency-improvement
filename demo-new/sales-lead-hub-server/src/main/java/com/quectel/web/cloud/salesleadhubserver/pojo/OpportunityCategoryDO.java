package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 商机-分类关联（opportunity_category）。N:M 关系真相源；无 id、复合主键 (opportunity_id, category_id)、无时间列。
 *
 * <p>MyBatis-Plus 单主键模型不支持真正的复合主键，此处将 opportunityId 标为 {@code IdType.INPUT} 仅为让 insert 正常工作；
 * <b>关系维护应走自定义条件（LambdaQueryWrapper 按 opportunityId 批量 delete/insert），不要用 selectById/removeById</b>（那只会按单列匹配，语义不完整）。</p>
 */
@Data
@TableName("opportunity_category")
public class OpportunityCategoryDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商机 FK（复合主键之一；INPUT 表示应用赋值，不生成雪花） */
    @TableId(value = "opportunity_id", type = IdType.INPUT)
    private Long opportunityId;

    /** 分类 FK（复合主键之一） */
    @TableField("category_id")
    private Long categoryId;
}
