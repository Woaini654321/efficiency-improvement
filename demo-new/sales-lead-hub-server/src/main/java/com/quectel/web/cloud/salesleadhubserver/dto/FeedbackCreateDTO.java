package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 吐槽发布入参。字段以前端 {@code FeedbackCreateParams} 实发 payload 为准（仅 title/content）。
 *
 * <p>anon_name / emoji / color 均由后端生成，前端不传、也不接受前端传入
 * （防伪造匿名昵称、防绕过默认色板）。</p>
 */
@Data
public class FeedbackCreateDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String content;
}
