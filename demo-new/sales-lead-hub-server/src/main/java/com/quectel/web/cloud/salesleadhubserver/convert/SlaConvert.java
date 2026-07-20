package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaRequestVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaTimelineVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SLA 列表项手写映射：DO + 实时派生结果 → VO。
 *
 * <p>派生值（slaStatus/escalationLevel/remainingText/deadline/timeline）由 service 借
 * {@link com.quectel.web.cloud.salesleadhubserver.service.SlaCalculator} 算好后传入，
 * convert 只负责扁平赋值，便于 VO 契约（snake_case + 日期格式）离线断言。</p>
 */
@Component
public class SlaConvert {

    public SlaRequestVO toRequestVO(OpportunityRequestDO d,
                                    String slaStatus,
                                    String escalationLevel,
                                    String remainingText,
                                    LocalDateTime deadline,
                                    List<SlaTimelineVO> timeline) {
        SlaRequestVO v = new SlaRequestVO();
        v.setRequestId(d.getId());
        v.setTitle(d.getTitle());
        v.setUrgency(d.getUrgency());
        v.setSlaStatus(slaStatus);
        v.setCreatedAt(d.getCreateTime());
        v.setDeadline(deadline);
        v.setRemainingText(remainingText);
        v.setResponseCount(d.getResponseCount());
        v.setEscalationLevel(escalationLevel);
        v.setPublisherName(d.getPublisherName());
        v.setEscalationTimeline(timeline);
        return v;
    }

    /** 时间线单行构造器（纯展示字符串）。 */
    public SlaTimelineVO timelineRow(String time, String desc, String notifyTo) {
        SlaTimelineVO r = new SlaTimelineVO();
        r.setTime(time);
        r.setDesc(desc);
        r.setNotifyTo(notifyTo);
        return r;
    }
}
