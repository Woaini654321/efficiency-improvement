package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 公告（announcement）。运营 PC-24/25；状态 draft/published/archived。含全部 4 审计列，继承 {@link BaseEntity}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("announcement")
public class AnnouncementDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 标题 */
    private String title;

    /** 富文本正文 */
    private String content;

    /** notice/policy/activity/other */
    private String type;

    /** high/normal */
    private String priority;

    /** 发布人 FK */
    private Long publisherId;

    /** 发布人姓名快照 */
    private String publisherName;

    /** 是否置顶 */
    private Integer isPinned;

    /** draft/published/archived */
    private String status;

    /** 浏览数 */
    private Integer viewCount;

    /** 是否启用横幅 */
    private Integer bannerEnabled;

    /** 乐观锁 */
    @Version
    private Integer version;

    /** 发布时间 */
    private LocalDateTime publishedAt;
}
