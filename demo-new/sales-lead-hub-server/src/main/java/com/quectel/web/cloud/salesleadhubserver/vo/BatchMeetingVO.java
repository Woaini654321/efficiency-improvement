package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批量发布向导 - 可选会议项。键与前端 {@code BatchMeetingDTO}（batch/types.ts）对齐：
 * meeting_id/name/meeting_date/recorder_name。
 *
 * <p>嵌套在 {@link BatchMetaVO} 内，但 @JsonNaming 类级策略不级联到嵌套对象，
 * 故本类必须自带 @JsonNaming，否则 meetingId/recorderName 出成 camelCase。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BatchMeetingVO {

    private Long meetingId;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime meetingDate;

    private String recorderName;
}
