package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 需求-邀请产品线关联（request_product_line）。N:M 关系真相源；无 id、复合主键 (request_id, product_line_id)、无时间列。
 *
 * <p>与 request_category 同范式：关系走本关联表（可索引反查「某产品线被哪些需求邀请」，支撑 §3.2 SLA L1 升级人解析），
 * 展示走 opportunity_request.invited_product_line_names 快照。</p>
 *
 * <p>MyBatis-Plus 单主键模型不支持真正的复合主键，此处将 requestId 标为 {@code IdType.INPUT} 仅为让 insert 正常工作；
 * <b>关系维护应走自定义条件（LambdaQueryWrapper 按 requestId 批量 delete/insert），不要用 selectById/removeById</b>。</p>
 */
@Data
@TableName("request_product_line")
public class RequestProductLineDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 需求 FK（复合主键之一；INPUT 表示应用赋值，不生成雪花） */
    @TableId(value = "request_id", type = IdType.INPUT)
    private Long requestId;

    /** 产品线 FK（复合主键之一） */
    @TableField("product_line_id")
    private Long productLineId;
}
