package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityPageVO;
import org.springframework.stereotype.Component;

/**
 * 手写 DTO/VO ⇄ DO 映射。
 *
 * <p>审计字段与 publisher* 一律不在此处赋值：前者由框架
 * {@code SecurityMetaObjectHandler} 填充，后者由 service 回填（含代发布分支）。</p>
 */
@Component
public class OpportunityConvert {

    public OpportunityDO toCreateDO(OpportunityCreateDTO dto) {
        OpportunityDO d = new OpportunityDO();
        d.setTitle(dto.getTitle());
        d.setType(dto.getType());
        d.setStatus(dto.getStatus());
        d.setSummary(dto.getSummary());
        d.setContent(dto.getContent());
        return d;
    }

    /**
     * 把可改字段与 version 灌入<b>已从库中加载</b>的 DO。
     *
     * <p>必须回填客户端带上来的 version 参与 {@code WHERE version=?}，
     * 沿用库里的会让任何陈旧提交都被当成最新提交放行。</p>
     */
    public void applyUpdate(OpportunityUpdateDTO dto, OpportunityDO d) {
        d.setTitle(dto.getTitle());
        d.setType(dto.getType());
        d.setStatus(dto.getStatus());
        d.setSummary(dto.getSummary());
        d.setContent(dto.getContent());
        d.setVersion(dto.getVersion());
    }

    public OpportunityPageVO toPageVO(OpportunityDO d) {
        OpportunityPageVO v = new OpportunityPageVO();
        v.setOpportunityId(d.getId());
        v.setTitle(d.getTitle());
        v.setSummary(d.getSummary());
        v.setType(d.getType());
        v.setStatus(d.getStatus());
        v.setPublisherName(d.getPublisherName());
        v.setPublisherDeptName(d.getPublisherDeptName());
        v.setCategoryNames(d.getCategoryNames());
        v.setViewCount(d.getViewCount());
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setCommentCount(d.getCommentCount());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    public OpportunityDetailVO toDetailVO(OpportunityDO d) {
        OpportunityDetailVO v = new OpportunityDetailVO();
        v.setOpportunityId(d.getId());
        v.setTitle(d.getTitle());
        v.setSummary(d.getSummary());
        v.setContent(d.getContent());
        v.setType(d.getType());
        v.setStatus(d.getStatus());
        v.setPublisherName(d.getPublisherName());
        v.setPublisherDeptName(d.getPublisherDeptName());
        v.setCategoryNames(d.getCategoryNames());
        v.setViewCount(d.getViewCount());
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setCommentCount(d.getCommentCount());
        v.setAttachments(d.getAttachments());
        v.setCreatedAt(d.getCreateTime());
        v.setUpdatedAt(d.getUpdateTime());
        v.setVersion(d.getVersion());
        return v;
    }
    // 两个 toXxxVO 逐字段显式赋值、不抽公共基类：契约类型保持扁平可读，
    // 任一 VO 增减字段时编译器能直接指到这里。
}
