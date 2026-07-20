package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人中心-我的收藏项。键对齐前端 adapter：collect_id/title/type/is_deleted/created_at。
 *
 * <p>目标已逻辑删/物理删的收藏在 service 层回查不到即丢弃，故存活项 is_deleted 恒 false。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ProfileCollectVO {

    /** 前端读 collect_id；用互动行 id */
    private Long collectId;

    private String title;

    /** opportunity / requirement */
    private String type;

    private Boolean isDeleted;

    /** 收藏时间（互动行 create_time） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
