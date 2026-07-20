package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 提交方案入参。字段以需求详情页「提交方案」抽屉的实发表单为准（不照抄 PRD）：
 * content(RichEditor) / attachments(QUpload) / emailRecipients(勾选组) /
 * customEmailRecipients(自定义邮箱 tags) / feishuSync(开关)。
 *
 * <p>requestId 用 {@link Long}：前端 id 是 string，Jackson 默认把 JSON 字符串强转 Long，
 * 与既有 RequirementUpdateDTO.id 的处理一致。</p>
 */
@Data
public class ResponseCreateDTO {

    /** 所属需求 id */
    @NotNull
    private Long requestId;

    /** 富文本方案正文（contenteditable 产出，可能较长，不加 @Size 收窄） */
    @NotBlank
    private String content;

    /** 附件（文件名/URL 列表），可空 */
    private List<String> attachments;

    /** 邮件通知人（勾选组：publisher/responders/followers），可空 */
    private List<String> emailRecipients;

    /** 自定义邮件通知人（前端 tags 输入），可空 */
    private List<String> customEmailRecipients;

    /** 是否同步飞书（前端开关布尔；service 落库转 0/1） */
    private Boolean feishuSync;
}
