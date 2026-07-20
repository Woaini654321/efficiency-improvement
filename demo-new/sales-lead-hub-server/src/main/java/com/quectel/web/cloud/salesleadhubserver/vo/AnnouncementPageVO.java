package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 前台公告列表项出参。对齐前端 {@code apis/notification/announcementAdapter.ts} 的 toItem 读取键。
 *
 * <p>类级 @JsonNaming 出 snake_case（禁全局策略）。与运营端 VO <b>刻意不互相继承</b>：
 * 前台契约更窄（不含 content/created_at/banner_enabled/version），列表卡片不渲染正文，
 * content 只在详情下发以省带宽。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AnnouncementPageVO {

    /** 前端 adapter 读的键名是 announcement_id（不是 id） */
    private Long announcementId;

    private String title;

    /** notice/policy/activity/other */
    private String type;

    /** draft/published/archived（前台恒为 published） */
    private String status;

    /** high/normal */
    private String priority;

    private Boolean isPinned;

    private String publisherName;

    private Integer viewCount;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedAt;
}
