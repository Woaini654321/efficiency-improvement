package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 新建需求入参，字段名对齐前端 {@code form/index.vue} 的 doPublish() 实发 payload。
 *
 * <p><b>刻意不含 publisherId/publisherName/departmentId/publisherDeptName</b>——
 * 全部由 service 从 SSO 上下文回填，防止客户端伪造发布人。</p>
 */
@Data
public class RequirementCreateDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String description;

    /** 前端不校验必填，后端保持一致不加 @NotBlank，避免前端能提交后端却 400。 */
    private String industry;

    @NotBlank
    @Pattern(regexp = "normal|urgent|critical")
    private String urgency;

    /** 前端实发字段名是 visibilityType；DB 列为 visibility_scope，由 convert 桥接。 */
    @NotBlank
    @Pattern(regexp = "all|dept|personnel")
    private String visibilityType;

    private List<String> visibilityValues;

    /** 前端 ID 全为 string（Long→String 约定），service 内转 Long。 */
    private List<String> categoryIds;
}
