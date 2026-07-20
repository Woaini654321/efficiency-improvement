package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.FeedbackCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.FeedbackDO;
import com.quectel.web.cloud.salesleadhubserver.vo.FeedbackVO;
import org.springframework.stereotype.Component;

/**
 * 手写 DTO/VO ⇄ DO 映射。
 *
 * <p>审计字段（含 create_by=真实作者）一律不在此处赋值：由框架
 * {@code SecurityMetaObjectHandler} 填充；anon_name/emoji/color 由 service 用
 * {@code FeedbackAnonymizer} 回填。<b>toVO 刻意不映射 create_by</b>——FeedbackVO
 * 从结构上无该字段，杜绝真实身份回泄。</p>
 */
@Component
public class FeedbackConvert {

    public FeedbackDO toCreateDO(FeedbackCreateDTO dto) {
        FeedbackDO d = new FeedbackDO();
        d.setTitle(dto.getTitle());
        d.setContent(dto.getContent());
        return d;
    }

    public FeedbackVO toVO(FeedbackDO d) {
        FeedbackVO v = new FeedbackVO();
        v.setFeedbackId(d.getId());
        v.setTitle(d.getTitle());
        v.setContent(d.getContent());
        v.setAnonName(d.getAnonName());
        v.setLikeCount(d.getLikeCount());
        v.setEmoji(d.getEmoji());
        v.setColor(d.getColor());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }
}
