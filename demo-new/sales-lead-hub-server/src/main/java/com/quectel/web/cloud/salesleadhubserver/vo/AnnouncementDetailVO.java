package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 前台公告详情出参。相较列表多下发 {@code content}（详情页 v-html 渲染富文本）。
 *
 * <p>与 {@link AnnouncementPageVO} 及运营端 VO 均<b>不互相继承</b>（扁平契约类型，
 * 任一侧字段漂移编译器能直接指到）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AnnouncementDetailVO {

    /** 前端 adapter 读的键名是 announcement_id（不是 id） */
    private Long announcementId;

    private String title;

    /** 富文本 HTML 正文，仅详情下发 */
    private String content;

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
