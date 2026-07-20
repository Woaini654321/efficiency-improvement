package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 关闭需求入参。路径 {@code requirement/close}，入参 {id, reason(可空)}。
 *
 * <p>reason 当前 notification/opportunity_request 均无对应列，暂不落库（仅供审计快照下期使用）。</p>
 */
@Data
public class RequirementCloseDTO {

    /** 需求 id */
    @NotNull
    private Long id;

    /** 关闭原因，可空 */
    private String reason;
}
