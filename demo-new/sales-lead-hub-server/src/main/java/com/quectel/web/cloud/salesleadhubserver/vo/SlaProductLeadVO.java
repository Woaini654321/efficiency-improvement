package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 催办元数据·产品线负责人候选（SLA L1 升级人）。id 为负责人 sys_user id（前端 string）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SlaProductLeadVO {

    /** 负责人 sys_user id */
    private Long id;

    /** 负责人姓名 */
    private String name;

    /** 所负责产品线名 */
    private String product;

    /** 负责人所属部门名 */
    private String dept;
}
