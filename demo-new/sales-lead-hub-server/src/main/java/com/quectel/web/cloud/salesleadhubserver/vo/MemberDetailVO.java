package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品线成员详情出参。与 {@link MemberPageVO} 刻意不互相继承——扁平契约类型，
 * 任一侧增减字段编译器能直接指到（理由见 OpportunityDetailVO）。
 *
 * <p>employee_id / department_name 由 service 从当前 sys_user 单行解析后回填，
 * 供成员详情展示；成员行本身只存 user_name 快照。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MemberDetailVO {

    /** 成员行主键 id */
    private Long memberId;

    private Long productLineId;

    /** 产品线名（service 回填） */
    private String productLineName;

    private Long userId;

    /** 成员姓名快照 */
    private String userName;

    /** 工号（service 从 sys_user 回填） */
    private String employeeId;

    /** 部门名（service 从 sys_user 回填） */
    private String departmentName;

    /** 1=产品线负责人(L1 升级人) */
    private Integer isOwner;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
