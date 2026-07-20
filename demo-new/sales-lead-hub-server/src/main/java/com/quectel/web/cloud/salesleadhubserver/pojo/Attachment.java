package com.quectel.web.cloud.salesleadhubserver.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * 附件（JSON 列内嵌结构，非独立表）。
 *
 * <p>契约 {@code {name,url,size}} 与前端 AttachmentDTO 逐字一致；字段全是
 * 单词小写，snake_case 序列化是 no-op，可直接用于 VO 出参。</p>
 */
@Data
public class Attachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件名 */
    private String name;

    /** 下载地址（后续接 oss-starter 预签名 URL） */
    private String url;

    /** 字节数 */
    private Long size;
}
