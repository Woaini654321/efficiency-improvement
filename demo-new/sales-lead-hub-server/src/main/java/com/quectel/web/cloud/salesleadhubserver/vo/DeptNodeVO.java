package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树节点出参。供需求可见性 dept 选择器使用。
 *
 * <p>类级 @JsonNaming 出 snake_case（{@code department_id / name / children}），禁全局策略
 * （理由见 OpportunityPageVO）。children 自嵌套，无子节点时为空数组而非 null。仅暴露
 * id/name/children，owner/sort_order 等不下发（选择器只需展示与选择）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeptNodeVO {

    /** 部门 id（Long→String 由全局序列化处理），序列化为 department_id */
    private Long departmentId;

    /** 部门名 */
    private String name;

    /** 子部门，按 sort_order 升序；无子节点时为空数组 */
    private List<DeptNodeVO> children = new ArrayList<>();
}
