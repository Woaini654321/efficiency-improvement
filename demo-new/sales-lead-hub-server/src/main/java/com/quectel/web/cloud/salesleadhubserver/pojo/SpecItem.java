package com.quectel.web.cloud.salesleadhubserver.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * 竞品情报规格键值对（JSON 列内嵌结构，非独立表）。
 *
 * <p>契约 {@code {label,value}} 与前端 IntelSpec 逐字一致；字段全是单词小写，
 * snake_case 序列化是 no-op，可直接用于 VO 出参（同 {@link Attachment} 模式）。</p>
 */
@Data
public class SpecItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 规格项名 */
    private String label;

    /** 规格项值 */
    private String value;
}
