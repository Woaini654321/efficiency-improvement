package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知分页入参。分页四字段继承 {@link BasePageDTO}。
 *
 * <p>过滤字段对齐前端 {@code NotificationPageParams}（keyword/type/isRead）。当前通知中心页
 * 是「全量拉取 + 客户端过滤/分组」模式（实发只有 pageNumber/pageSize=999），
 * 这些过滤为契约预留，后端照常支持，前端何时下沉过滤不影响本契约。</p>
 *
 * <p>{@code userId} 刻意不作为入参——归属强制取当前登录人 id，防越权查他人通知。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationPageDTO extends BasePageDTO {

    /** 标题模糊搜索，可空 */
    private String keyword;

    /** 通知类型：publish/response/adopt/system/mention/subscribe/force_confirm 等，可空 */
    private String type;

    /**
     * 已读筛选（对齐前端 tab）：'read'=已读、'unread'=未读、空/其它=全部。
     * 用字符串而非 Boolean：前端 segmented 值是字符串，空串代表「全部」。
     */
    private String isRead;
}
