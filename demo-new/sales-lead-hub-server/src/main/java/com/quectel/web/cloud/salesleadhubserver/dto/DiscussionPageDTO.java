package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 讨论帖分页入参。分页四字段继承 {@link BasePageDTO}，本类补 keyword/topic。
 *
 * <p>字段以前端 {@code DiscussionPageParams}（keyword? / topic?）为准。keyword 模糊
 * 匹配标题+正文，topic 精确过滤。DTO 保持 camelCase（全局不开 snake_case）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiscussionPageDTO extends BasePageDTO {

    /** 关键词，模糊匹配 title/content，可空 */
    private String keyword;

    /** 话题精确过滤：business/solution/experience/industry/complaint，可空 */
    private String topic;
}
