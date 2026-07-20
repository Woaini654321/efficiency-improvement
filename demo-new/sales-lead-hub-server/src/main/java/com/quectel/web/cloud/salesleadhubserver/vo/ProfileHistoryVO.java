package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人中心-浏览记录项。键对齐前端 adapter：history_id/title/type/viewed_at。
 *
 * <p>目标回查不到（逻辑删/物理删）即丢弃。按 viewed_at 倒序。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileHistoryVO {

    /** 前端读 history_id；用 view_log 行 id */
    private Long historyId;

    private String title;

    /** opportunity / requirement */
    private String type;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime viewedAt;
}
