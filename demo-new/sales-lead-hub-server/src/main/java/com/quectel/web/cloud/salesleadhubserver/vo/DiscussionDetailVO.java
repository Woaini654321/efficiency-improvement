package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 讨论帖详情出参。较列表项多一个 {@code comments[]}（递归回帖树）。
 *
 * <p>与 {@link DiscussionPageVO} 刻意不互相继承——扁平契约类型，理由同 OpportunityDetailVO。
 * 不带 version：discussion_post 无乐观锁列，详情页也无编辑入口（发帖是新建、回帖是纯前端本地态）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DiscussionDetailVO {

    /** 前端 adapter 读的键名是 post_id（不是 id） */
    private Long postId;

    private String title;

    private String content;

    /** business/solution/experience/industry/complaint */
    private String topic;

    private String authorName;

    private Integer replyCount;

    private Integer viewCount;

    private Boolean isHot;

    private List<String> tags;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 递归回帖树（内存组树，children 按 create_time 正序） */
    private List<DiscussionCommentVO> comments;
}
