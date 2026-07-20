package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 个人中心-用户段。类级 @JsonNaming 出 snake_case。
 *
 * <p>只下发前端 adapter 实读的四个字段（name/dept_name/role_name/employee_no）。
 * <b>不下发 phone</b>——个人中心页不展示手机号（用户卡只显示姓名/角色/部门/工号），
 * 故无需 §6 脱敏，直接不返回是最稳的防泄漏。role_name 是可读角色名（convert 把
 * sys_user.role 码值映射为中文），前端直接渲染不再 t() 翻译。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileUserVO {

    private String name;

    /** 部门名快照（sys_user.department_name） */
    private String deptName;

    /** 可读角色名（销售/产品经理/管理员） */
    private String roleName;

    /** 工号（sys_user.employee_id） */
    private String employeeNo;
}
