package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 运营端公告详情出参（operation/announce/detail）。字段形态与
 * {@link OperationAnnouncePageVO} 一致（前端 detail 与 list 复用同一 toItem adapter），
 * 但按配方保持两个扁平 VO 不互相继承——契约漂移编译期可见。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OperationAnnounceDetailVO {

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

    /** 富文本 HTML 正文 */
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
