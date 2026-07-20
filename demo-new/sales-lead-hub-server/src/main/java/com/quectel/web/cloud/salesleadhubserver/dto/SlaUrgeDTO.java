package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 催办入参。字段以前端 {@code sla/index.vue} 的 {@code urgeSlaRequest} 实发为准：
 * {@code { id, targets, methods, remark? }}。
 *
 * <p><b>targets 是接收人「类别」而非用户 id</b>（前端复选框值）：publisher/supervisor/product_lead，
 * 由 service 按当前需求解析成本地 sys_user（发布人 / 部门负责人 / 产品线负责人）。
 * methods 是渠道：in_app/feishu/email。</p>
 */
@Data
public class SlaUrgeDTO {

    /** 目标需求 id（前端 string，service 转 Long） */
    @NotBlank
    private String id;

    /** 接收人类别集：publisher/supervisor/product_lead */
    @NotEmpty
    private List<String> targets;

    /** 催办渠道集：in_app/feishu/email */
    @NotEmpty
    private List<String> methods;

    /** 催办备注，可空（当前 notification 表无对应列，暂不落库） */
    private String remark;
}
