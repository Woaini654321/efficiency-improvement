package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementPageVO;
import org.springframework.stereotype.Component;

/**
 * 手写 DTO/VO ⇄ DO 映射。
 *
 * <p>审计字段（createBy/createTime/…）与 publisher* 一律不在此处赋值：
 * 前者由框架 {@code SecurityMetaObjectHandler} 填充，后者由 service 从 SSO 上下文回填。</p>
 */
@Component
public class RequirementConvert {

    public OpportunityRequestDO toCreateDO(RequirementCreateDTO dto) {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setTitle(dto.getTitle());
        d.setDescription(dto.getDescription());
        d.setIndustry(dto.getIndustry());
        d.setUrgency(dto.getUrgency());
        d.setVisibilityScope(dto.getVisibilityType());   // 契约名 -> DB 名，单一桥接点
        d.setVisibilityValues(dto.getVisibilityValues());
        return d;
    }

    /**
     * 把可改字段与 version 灌入<b>已从库中加载</b>的 DO。
     *
     * <p>乐观锁依赖实体上的 version 值参与 {@code WHERE version=?}，故必须回填客户端带上来的
     * version，而不是沿用库里的——否则任何陈旧提交都会被当成最新提交放行。</p>
     */
    public void applyUpdate(RequirementUpdateDTO dto, OpportunityRequestDO d) {
        d.setTitle(dto.getTitle());
        d.setDescription(dto.getDescription());
        d.setIndustry(dto.getIndustry());
        d.setUrgency(dto.getUrgency());
        d.setVisibilityScope(dto.getVisibilityType());
        d.setVisibilityValues(dto.getVisibilityValues());
        d.setVersion(dto.getVersion());
    }

    public RequirementPageVO toPageVO(OpportunityRequestDO d) {
        RequirementPageVO v = new RequirementPageVO();
        v.setRequestId(d.getId());
        v.setTitle(d.getTitle());
        v.setDescription(d.getDescription());
        v.setIndustry(d.getIndustry());
        v.setUrgency(d.getUrgency());
        v.setStatus(d.getStatus());
        v.setPublisherName(d.getPublisherName());
        v.setPublisherDeptName(d.getPublisherDeptName());
        v.setCategoryNames(d.getCategoryNames());
        v.setVisibilityType(d.getVisibilityScope());
        v.setVisibilityValues(d.getVisibilityValues());
        v.setInvitedProductLineNames(d.getInvitedProductLineNames());
        v.setAdoptedResponseId(toIdString(d.getAdoptedResponseId()));
        v.setSlaStatus(d.getSlaStatus());
        v.setEscalationLevel(d.getEscalationLevel());
        v.setResponseCount(d.getResponseCount());
        v.setViewCount(d.getViewCount());
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setCommentCount(d.getCommentCount());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    public RequirementDetailVO toDetailVO(OpportunityRequestDO d) {
        RequirementDetailVO v = new RequirementDetailVO();
        v.setRequestId(d.getId());
        v.setTitle(d.getTitle());
        v.setDescription(d.getDescription());
        v.setIndustry(d.getIndustry());
        v.setUrgency(d.getUrgency());
        v.setStatus(d.getStatus());
        v.setPublisherName(d.getPublisherName());
        v.setPublisherDeptName(d.getPublisherDeptName());
        v.setCategoryNames(d.getCategoryNames());
        v.setVisibilityType(d.getVisibilityScope());
        v.setVisibilityValues(d.getVisibilityValues());
        v.setInvitedProductLineNames(d.getInvitedProductLineNames());
        v.setAdoptedResponseId(toIdString(d.getAdoptedResponseId()));
        v.setSlaStatus(d.getSlaStatus());
        v.setEscalationLevel(d.getEscalationLevel());
        v.setResponseCount(d.getResponseCount());
        v.setViewCount(d.getViewCount());
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setCommentCount(d.getCommentCount());
        v.setCreatedAt(d.getCreateTime());
        v.setUpdatedAt(d.getUpdateTime());
        v.setVersion(d.getVersion());
        return v;
    }

    // 两个 toXxxVO 各自逐字段显式赋值，字段集相同也不抽公共基类/不用反射：
    // 契约类型保持扁平可读，任一 VO 增减字段时编译器能直接指到这里。

    private String toIdString(Long id) {
        return id == null ? null : String.valueOf(id);
    }
}
