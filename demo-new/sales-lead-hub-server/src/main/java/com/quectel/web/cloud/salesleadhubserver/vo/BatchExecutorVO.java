package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 批量发布向导 - 可选执行人项。键与前端 {@code BatchExecutorDTO}（batch/types.ts）对齐：
 * user_id/name/dept_name。
 *
 * <p>同 {@link BatchMeetingVO}：嵌套对象需自带 @JsonNaming。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BatchExecutorVO {

    private Long userId;

    private String name;

    private String deptName;
}
