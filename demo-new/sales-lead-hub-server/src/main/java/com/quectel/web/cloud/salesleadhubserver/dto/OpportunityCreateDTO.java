package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 商机创建入参。字段以前端 {@code buildParams()} 实发 payload 为准。
 *
 * <p>刻意不声明前端会发的 {@code industry}/{@code keywords}：opportunity 表
 * 无对应列、出参 DTO 亦无此二字段（页面不回显），属原型残留的 write-only 字段，
 * Jackson 反序列化静默忽略即可，不落库。若产品后续要求展示，先加列再声明。</p>
 *
 * <p>{@code status} 只收 draft/published（archived 只能走 changeStatus），
 * 枚举合法性与「发布必填分类/正文」在 service 层按状态条件校验——
 * 存草稿只要求标题，与前端行为一致，故这里不能对 content/categoryIds 加硬约束。</p>
 */
@Data
public class OpportunityCreateDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    /** product_info / solution / success_case */
    @NotBlank
    private String type;

    /** draft / published；由 service 校验取值 */
    @NotBlank
    private String status;

    /** 叶子分类 id（string，Long→String 约定）；发布时 1~5 个，草稿可空 */
    private List<String> categoryIds;

    @Size(max = 500)
    private String summary;

    /** 富文本 HTML（MEDIUMTEXT），勿加 @Size 收窄 */
    private String content;

    /** 代发布目标用户 id（string）；空=本人发布 */
    private String publisherId;
}
