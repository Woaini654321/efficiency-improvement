package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运营内容审核分页入参。分页基础字段继承 {@link BasePageDTO}（注意基类无 keyword，本类自声明）。
 *
 * <p>跨 opportunity 与 opportunity_request 两张表的联合审核视图。前端实发（运营审核页
 * {@code load()}）只带 pageNumber/pageSize（全量拉取 + 客户端过滤），未传 contentType，
 * 故不传时后端合并两表；传 opportunity/request 时分流到单表。keyword/status 为契约预留，
 * 后端照常支持。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuditPageDTO extends BasePageDTO {

    /** 关键词模糊搜索（标题），可空 */
    private String keyword;

    /** 内容类型：opportunity=商机 / request=需求；为空则合并两表 */
    private String contentType;

    /**
     * 状态过滤，可空。opportunity 取值 published/archived；request 取值
     * Pending/Collecting/Adopted/Closed。取值合法性不在此强校验（跨表语义不同）。
     */
    private String status;
}
