package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 前台公告分页入参。路径 {@code announcement/page}，对齐前端
 * {@code apis/notification/announcementApi.ts} 的 {@code AnnouncementPageParams}。
 *
 * <p>前台<b>不接受 status</b>：前台恒定只看 {@code published}，状态过滤由 service 强制，
 * 不给客户端可选参数（避免前台越权拉到草稿/已归档）。分页四字段继承 {@link BasePageDTO}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnouncementPageDTO extends BasePageDTO {

    /** 标题关键词模糊搜索，可空 */
    private String keyword;

    /** notice/policy/activity/other，可空 */
    private String type;
}
