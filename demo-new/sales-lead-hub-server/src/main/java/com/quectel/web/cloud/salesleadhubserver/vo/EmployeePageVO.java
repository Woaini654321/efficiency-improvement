package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 人员选择器列表项出参。类级 @JsonNaming 出 snake_case（禁全局策略，理由见 OpportunityPageVO）。
 *
 * <p><b>刻意只声明 id/name/employee_id/department_name 四个字段</b>：人员选择器只需展示与
 * 选人，phone/email/avatar 等敏感字段一律不下发（最小暴露面）。无时间字段，故无 @JsonFormat。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeePageVO {

    /** 用户 id（= UAA 用户 id，Long→String 由全局序列化处理） */
    private Long id;

    /** 姓名 */
    private String name;

    /** 工号 */
    private String employeeId;

    /** 部门名快照 */
    private String departmentName;
}
