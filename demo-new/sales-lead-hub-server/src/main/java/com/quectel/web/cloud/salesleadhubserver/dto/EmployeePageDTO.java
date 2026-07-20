package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 人员选择器分页入参。分页四字段继承 {@link BasePageDTO}。
 *
 * <p>供可见性配置 / 任务指派等场景的人员选择器使用，keyword 同时模糊匹配
 * 姓名 name 与工号 employee_id。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeePageDTO extends BasePageDTO {

    /** 关键词，模糊匹配 name 或 employee_id */
    private String keyword;
}
