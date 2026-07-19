package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 浏览记录（view_log）。24h 去重（唯一键 user+target + 应用层 TTL）。
 *
 * <p>注意：本表<b>无</b> create_time 列，仅有业务字段 viewed_at（浏览时间，作为 24h 去重基准，由应用层写入），
 * 故不加审计填充注解、也不继承 {@link BaseEntity}。</p>
 */
@Data
@TableName("view_log")
public class ViewLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 浏览用户 FK */
    private Long userId;

    /** Opportunity/Request */
    private String targetType;

    /** 目标 id */
    private Long targetId;

    /** 浏览时间(24h 去重基准)，业务写入 */
    private LocalDateTime viewedAt;
}
