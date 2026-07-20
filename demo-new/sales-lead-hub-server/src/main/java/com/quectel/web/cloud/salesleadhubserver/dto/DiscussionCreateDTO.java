package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 讨论帖创建入参。字段以前端 {@code DiscussionCreateParams} 实发 payload 为准：
 * 恰好 {@code {title, topic, content}} 三个（发布页 {@code handleSubmit()} 传参）。
 *
 * <p>刻意<b>不声明 tags</b>：前端发帖页不采集标签，故创建时 tags 落空数组
 * （由 service 兜底），待运营后续维护。topic 取值合法性在 service 层按枚举校验
 * （前端是 a-segmented 固定 5 选项，理论不会越界，但后端仍须 fail-closed）。</p>
 *
 * <p>{@code content} 是富文本 HTML，勿加 {@code @Size} 收窄。</p>
 */
@Data
public class DiscussionCreateDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    /** business/solution/experience/industry/complaint；由 service 校验取值 */
    @NotBlank
    private String topic;

    /** 富文本 HTML 正文 */
    @NotBlank
    private String content;
}
