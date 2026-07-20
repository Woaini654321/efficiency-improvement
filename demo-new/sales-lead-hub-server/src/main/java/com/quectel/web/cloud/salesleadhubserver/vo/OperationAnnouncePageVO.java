package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运营端公告列表项出参。对齐前端 {@code apis/announce/announceAdapter.ts} 的 toItem 读取键。
 *
 * <p>运营列表页一次性拉全量（pageSize 999）后本地预览，openPreview 直接取列表行的
 * content，openEdit 直接回填列表行的字段并随 update 提交 version —— 故运营列表 VO 是
 * <b>全字段</b>（含 content/created_at/banner_enabled/version），与前台窄契约刻意不继承。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OperationAnnouncePageVO {

    /** 前端 adapter 读的键名是 announcement_id（不是 id） */
    private Long announcementId;

    private String title;

    /** notice/policy/activity/other */
    private String type;

    /** draft/published/archived */
    private String status;

    /** high/normal */
    private String priority;

    private Boolean isPinned;

    private String publisherName;

    private Integer viewCount;

    /** 富文本 HTML 正文：运营列表页预览抽屉直接读此字段（不再单独请求详情） */
    private String content;

    private Boolean bannerEnabled;

    /** 编辑页回填后随 update 提交，参与乐观锁 */
    private Integer version;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedAt;
}
