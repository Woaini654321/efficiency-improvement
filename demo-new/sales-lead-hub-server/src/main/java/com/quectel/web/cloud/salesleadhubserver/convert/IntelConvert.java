package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelSubmitDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CompetitorIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.IndustryIntelDO;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelPageVO;
import org.springframework.stereotype.Component;

/**
 * 手写 DTO/VO ⇄ DO 映射（竞品 + 行业）。
 *
 * <p>审计字段与 submitter* 一律不在此处赋值：前者由框架 {@code SecurityMetaObjectHandler}
 * 填充，后者由 service 从本地 sys_user 回填（防客户端伪造提交人）。</p>
 */
@Component
public class IntelConvert {

    // ---------- 竞品情报 ----------

    /**
     * submit → DO：content 落 overview 列（submit 页只提供一段正文，analysis/impact 留空）。
     * submitter* 与计数初值由 service 负责，convert 不碰。
     */
    public CompetitorIntelDO toSubmitDO(CompetitorIntelSubmitDTO dto) {
        CompetitorIntelDO d = new CompetitorIntelDO();
        d.setBrand(dto.getBrand());
        d.setProduct(dto.getProduct());
        d.setIntelType(dto.getIntelType());
        d.setSource(dto.getSource());
        d.setTitle(dto.getTitle());
        d.setOverview(dto.getContent());
        return d;
    }

    public CompetitorIntelPageVO toCompetitorPageVO(CompetitorIntelDO d) {
        CompetitorIntelPageVO v = new CompetitorIntelPageVO();
        v.setIntelId(d.getId());
        v.setBrand(d.getBrand());
        v.setProduct(d.getProduct());
        v.setIntelType(d.getIntelType());
        v.setTitle(d.getTitle());
        v.setSummary(d.getSummary());
        v.setSource(d.getSource());
        v.setSubmitterName(d.getSubmitterName());
        // 列表卡片直接展示三计数（否则前端 adapter 的 ?? 0 兜底会恒显 0）
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setViewCount(d.getViewCount());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    public CompetitorIntelDetailVO toCompetitorDetailVO(CompetitorIntelDO d) {
        CompetitorIntelDetailVO v = new CompetitorIntelDetailVO();
        v.setIntelId(d.getId());
        v.setBrand(d.getBrand());
        v.setProduct(d.getProduct());
        v.setIntelType(d.getIntelType());
        v.setTitle(d.getTitle());
        v.setSummary(d.getSummary());
        v.setSource(d.getSource());
        v.setSubmitterName(d.getSubmitterName());
        v.setOverview(d.getOverview());
        v.setAnalysis(d.getAnalysis());
        v.setImpact(d.getImpact());
        v.setSpecs(d.getSpecs());
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setViewCount(d.getViewCount());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    // ---------- 行业情报 ----------

    public IndustryIntelPageVO toIndustryPageVO(IndustryIntelDO d) {
        IndustryIntelPageVO v = new IndustryIntelPageVO();
        v.setIntelId(d.getId());
        v.setIndustry(d.getIndustry());
        v.setTitle(d.getTitle());
        v.setSummary(d.getSummary());
        v.setSource(d.getSource());
        // 列表卡片直接展示三计数（否则前端 adapter 的 ?? 0 兜底会恒显 0）
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setViewCount(d.getViewCount());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    public IndustryIntelDetailVO toIndustryDetailVO(IndustryIntelDO d) {
        IndustryIntelDetailVO v = new IndustryIntelDetailVO();
        v.setIntelId(d.getId());
        v.setIndustry(d.getIndustry());
        v.setTitle(d.getTitle());
        v.setSummary(d.getSummary());
        v.setSource(d.getSource());
        v.setOverview(d.getOverview());
        v.setAnalysis(d.getAnalysis());
        v.setImpact(d.getImpact());
        v.setKeyPoints(d.getKeyPoints());
        v.setLikeCount(d.getLikeCount());
        v.setCollectCount(d.getCollectCount());
        v.setViewCount(d.getViewCount());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }
    // 各 toXxxVO 逐字段显式赋值、不抽公共基类：契约类型保持扁平可读，
    // 任一 VO 增减字段时编译器能直接指到这里。
}
