package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 需求详情里方案响应数组的元素。键名逐一对齐前端
 * {@code requirementAdapter.ts} 的 {@code mapResponses} 实读字段：
 * response_id / responder_name / responder_dept_name / content / is_adopted /
 * created_at / product_line_name / files。
 *
 * <p>类级 snake_case；时间字段 @JsonFormat 防 ISO 'T' 致前端 NaN。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequirementResponseVO {

    /** 方案 id（Long→String，前端按 string 用） */
    private String responseId;

    /** 响应人姓名快照 */
    private String responderName;

    /** 响应部门名快照 */
    private String responderDeptName;

    /** 富文本方案正文 */
    private String content;

    /** 是否已被采纳（DO isAdopted 1→true） */
    private Boolean isAdopted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 响应人所属产品线名。solution_response 表无该快照列，本期恒 null，
     * 前端 {@code ?? ''} 兜底；下期若要「受邀标记」再补来源。
     */
    private String productLineName;

    /** 附件（对应 DO attachments） */
    private List<String> files;
}
