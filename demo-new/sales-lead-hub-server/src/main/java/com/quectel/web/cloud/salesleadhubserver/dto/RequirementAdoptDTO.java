package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 采纳方案入参。路径 {@code requirement/adopt}，入参 {id(需求id), responseId(方案id)}。
 */
@Data
public class RequirementAdoptDTO {

    /** 需求 id */
    @NotNull
    private Long id;

    /** 被采纳的方案 id */
    @NotNull
    private Long responseId;
}
