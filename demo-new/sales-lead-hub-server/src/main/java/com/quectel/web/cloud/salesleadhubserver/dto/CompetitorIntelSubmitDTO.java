package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 竞品情报提交入参。字段以前端 {@code intel/submit/index.vue} 的 {@code handleSubmit()}
 * 实发 payload 为准：必填 brand/source/title/content，可选 product/intelType。
 *
 * <p>{@code content} 是富文本 HTML，落 {@code overview} 列（submit 页只提供一段正文，
 * analysis/impact 留空，待运营下期补充）。submitter 快照由 service 从当前登录人
 * 本地 sys_user 回填，前端无从传入。</p>
 */
@Data
public class CompetitorIntelSubmitDTO {

    @NotBlank
    @Size(max = 64)
    private String brand;

    @Size(max = 128)
    private String product;

    /** new_product/price_change/customer_case/other，可空 */
    private String intelType;

    @NotBlank
    @Size(max = 128)
    private String source;

    @NotBlank
    @Size(max = 200)
    private String title;

    /** 富文本 HTML，落 overview 列，勿加 @Size 收窄 */
    @NotBlank
    private String content;
}
