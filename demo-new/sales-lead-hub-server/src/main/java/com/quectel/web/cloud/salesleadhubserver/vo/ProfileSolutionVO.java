package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人中心-我提交的方案项。键对齐前端 adapter：
 * solution_id/title/request_title/request_id/summary/adopter_name/adopter_dept_name/adopted_at/is_best。
 *
 * <p>⚠️ solution_response 表<b>无 title/summary 列</b>，二者由富文本 content 去标签截断派生；
 * adopter_* 取所属需求发布人（采纳发生在需求侧）；is_best=该方案被需求采纳（request.adopted_response_id==本方案）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileSolutionVO {

    /** 前端读 solution_id */
    private Long solutionId;

    /** content 去标签截断派生（无 title 列） */
    private String title;

    private String requestTitle;

    private Long requestId;

    /** content 去标签截断派生（无 summary 列） */
    private String summary;

    private String adopterName;

    private String adopterDeptName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime adoptedAt;

    private Boolean isBest;
}
