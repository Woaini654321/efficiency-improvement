package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运营端公告分页入参。路径 {@code operation/announce/page}，对齐前端
 * {@code apis/announce/announceApi.ts} 的 {@code AnnouncePageParams}。
 *
 * <p>与前台不同：运营端可按 status 过滤，且不过滤状态时返回<b>全部含草稿</b>。
 * 分页四字段继承 {@link BasePageDTO}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationAnnouncePageDTO extends BasePageDTO {

    /** 标题关键词模糊搜索，可空 */
    private String keyword;

    /** notice/policy/activity/other，可空 */
    private String type;

    /** draft/published/archived，可空（空=全部状态） */
    private String status;
}
