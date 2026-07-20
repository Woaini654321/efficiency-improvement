package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 公告创建入参。字段以前端 {@code AnnounceCreateParams} 实发 payload 为准
 * （运营页 {@code createAnnounce({ ...formModel })}），<b>不照抄 PRD</b>。
 *
 * <p><b>payload 刻意不含 status</b>：前端存草稿与发布走的是同一个 create 调用
 * （两条路径都只发 title/type/priority/isPinned/bannerEnabled/content）。因此后端
 * create 一律落 {@code draft}（与 schema 默认一致），发布走独立的 changeStatus 端点。
 * 详见 {@code AnnouncementServiceImpl#create}。</p>
 *
 * <p>富文本 {@code content} 是 MEDIUMTEXT，<b>不加 @Size 收窄</b>；发布必填校验在
 * changeStatus 时按状态做（存草稿只要求标题非空）。</p>
 */
@Data
public class AnnounceCreateDTO {

    @NotBlank
    @Size(max = 200)
    private String title;

    /** notice/policy/activity/other */
    private String type;

    /** high/normal */
    private String priority;

    /** 是否置顶（前端 a-switch 布尔），落库转 0/1 */
    private Boolean isPinned;

    /** 是否启用横幅（前端 a-switch 布尔），落库转 0/1 */
    private Boolean bannerEnabled;

    /** 富文本 HTML（MEDIUMTEXT），勿加 @Size 收窄 */
    private String content;
}
