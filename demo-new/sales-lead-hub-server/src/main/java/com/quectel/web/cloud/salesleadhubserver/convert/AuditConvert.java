package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditPageVO;
import org.springframework.stereotype.Component;

/**
 * 运营内容审核视图映射：把结构不同的 opportunity 与 opportunity_request 两表 DO
 * 各自转成统一的 {@link AuditPageVO}。手写、不引 MapStruct。
 *
 * <p>content_type 是分流标识：{@link #toPageVO(OpportunityDO)} 恒置 opportunity，
 * {@link #toPageVO(OpportunityRequestDO)} 恒置 request。两表缺失的展示字段按 VO javadoc
 * 约定下发 null / 默认值（前端 adapter 有 {@code ??} 兜底）。</p>
 */
@Component
public class AuditConvert {

    public static final String TYPE_OPPORTUNITY = "opportunity";
    public static final String TYPE_REQUEST = "request";

    private static final String URGENCY_NORMAL = "normal";

    /** 商机行 → 审核视图。商机表无 urgency/industry 列，urgency 落 normal、industry 落 null。 */
    public AuditPageVO toPageVO(OpportunityDO d) {
        AuditPageVO v = new AuditPageVO();
        v.setAuditId(d.getId());
        v.setTitle(d.getTitle());
        v.setContentType(TYPE_OPPORTUNITY);
        v.setPublisherName(d.getPublisherName());
        v.setStatus(d.getStatus());
        v.setIsPinned(Boolean.TRUE.equals(d.getIsPinned()));
        v.setPublishedAt(d.getCreateTime());
        v.setSortNo(d.getSortNo() == null ? 0 : d.getSortNo());
        v.setUrgency(URGENCY_NORMAL);
        return v;
    }

    /** 需求行 → 审核视图。需求表有 urgency/industry 列，直取。 */
    public AuditPageVO toPageVO(OpportunityRequestDO d) {
        AuditPageVO v = new AuditPageVO();
        v.setAuditId(d.getId());
        v.setTitle(d.getTitle());
        v.setContentType(TYPE_REQUEST);
        v.setPublisherName(d.getPublisherName());
        v.setStatus(d.getStatus());
        v.setIsPinned(Boolean.TRUE.equals(d.getIsPinned()));
        v.setPublishedAt(d.getCreateTime());
        v.setSortNo(d.getSortNo() == null ? 0 : d.getSortNo());
        v.setUrgency(d.getUrgency() == null ? URGENCY_NORMAL : d.getUrgency());
        v.setIndustry(d.getIndustry());
        return v;
    }
}
