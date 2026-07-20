package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelSubmitDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CompetitorIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.IndustryIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SpecItem;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelPageVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 情报中心契约测试：submit DTO→DO 映射 + VO 离线序列化门禁
 * （snake_case 键名 + 日期格式 + specs/key_points JSON 列序列化）。
 */
class IntelConvertTest {

    private final IntelConvert convert = new IntelConvert();

    @Test
    void toSubmitDO_maps_content_to_overview_and_leaves_submitter_alone() {
        CompetitorIntelSubmitDTO dto = new CompetitorIntelSubmitDTO();
        dto.setBrand("Telit");
        dto.setProduct("FN990");
        dto.setIntelType("new_product");
        dto.setSource("Telit 官网新闻");
        dto.setTitle("Telit 发布 FN990");
        dto.setContent("<p>正文概述</p>");

        CompetitorIntelDO d = convert.toSubmitDO(dto);

        assertEquals("Telit", d.getBrand());
        assertEquals("FN990", d.getProduct());
        assertEquals("new_product", d.getIntelType());
        // content 落 overview 列；analysis/impact 留空待运营补充
        assertEquals("<p>正文概述</p>", d.getOverview());
        assertNull(d.getAnalysis());
        assertNull(d.getImpact());
        // submitter* 由 service 从本地 sys_user 回填，convert 不碰（防伪造提交人）
        assertNull(d.getSubmitterId());
        assertNull(d.getSubmitterName());
    }

    @Test
    void competitorDetailVO_serializes_snake_case_safe_date_and_specs_json() throws Exception {
        CompetitorIntelDO d = new CompetitorIntelDO();
        d.setId(973010000000000001L);
        d.setBrand("Telit");
        d.setIntelType("new_product");
        d.setTitle("Telit 发布 FN990");
        d.setSubmitterName("张伟");
        SpecItem s = new SpecItem();
        s.setLabel("平台");
        s.setValue("高通 SDX62");
        d.setSpecs(Collections.singletonList(s));
        d.setLikeCount(42);
        d.setViewCount(326);
        d.setCreateTime(LocalDateTime.of(2026, 7, 11, 9, 30, 0));

        CompetitorIntelDetailVO vo = convert.toCompetitorDetailVO(d);
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        // 前端 adapter 读的是 intel_id、intel_type、submitter_name、like_count、view_count
        assertTrue(json.contains("\"intel_id\""), json);
        assertTrue(json.contains("\"intel_type\""), json);
        assertTrue(json.contains("\"submitter_name\""), json);
        assertTrue(json.contains("\"like_count\""), json);
        assertTrue(json.contains("\"view_count\""), json);
        assertFalse(json.contains("intelType"), "不得出现 camelCase：" + json);
        // specs JSON 列：{label,value} 单词小写，snake 化是 no-op
        assertTrue(json.contains("\"specs\""), json);
        assertTrue(json.contains("\"label\":\"平台\""), json);
        assertTrue(json.contains("\"value\":\"高通 SDX62\""), json);
        // 日期安全格式
        assertTrue(json.contains("\"2026-07-11 09:30:00\""), json);
        assertFalse(json.contains("2026-07-11T09:30"), "禁 ISO 'T' 分隔：" + json);
    }

    @Test
    void competitorPageVO_serializes_three_counts_so_list_cards_are_not_stuck_at_zero() throws Exception {
        CompetitorIntelDO d = new CompetitorIntelDO();
        d.setId(973010000000000001L);
        d.setBrand("Telit");
        d.setIntelType("new_product");
        d.setTitle("Telit 发布 FN990");
        d.setLikeCount(42);
        d.setCollectCount(18);
        d.setViewCount(326);
        d.setCreateTime(LocalDateTime.of(2026, 7, 11, 9, 30, 0));

        CompetitorIntelPageVO vo = convert.toCompetitorPageVO(d);
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        // 列表卡片三计数必须下发，否则前端 adapter 的 ?? 0 兜底会掩盖缺键、恒显 0
        assertTrue(json.contains("\"like_count\":42"), json);
        assertTrue(json.contains("\"collect_count\":18"), json);
        assertTrue(json.contains("\"view_count\":326"), json);
        assertFalse(json.contains("likeCount"), "不得出现 camelCase：" + json);
    }

    @Test
    void industryPageVO_serializes_three_counts_so_list_cards_are_not_stuck_at_zero() throws Exception {
        IndustryIntelDO d = new IndustryIntelDO();
        d.setId(974010000000000001L);
        d.setIndustry("trend");
        d.setTitle("eRedCap 加速轻量 IoT 市场分层");
        d.setLikeCount(56);
        d.setCollectCount(32);
        d.setViewCount(512);
        d.setCreateTime(LocalDateTime.of(2026, 7, 11, 10, 0, 0));

        IndustryIntelPageVO vo = convert.toIndustryPageVO(d);
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        assertTrue(json.contains("\"like_count\":56"), json);
        assertTrue(json.contains("\"collect_count\":32"), json);
        assertTrue(json.contains("\"view_count\":512"), json);
        assertFalse(json.contains("collectCount"), "不得出现 camelCase：" + json);
    }

    @Test
    void industryDetailVO_serializes_snake_case_and_key_points_json() throws Exception {
        IndustryIntelDO d = new IndustryIntelDO();
        d.setId(974010000000000001L);
        d.setIndustry("trend");
        d.setTitle("eRedCap 加速轻量 IoT 市场分层");
        d.setKeyPoints(Arrays.asList("面向中低速率场景", "与 Cat.1 bis 互补"));
        d.setLikeCount(56);
        d.setViewCount(512);
        d.setCreateTime(LocalDateTime.of(2026, 7, 11, 10, 0, 0));

        IndustryIntelDetailVO vo = convert.toIndustryDetailVO(d);
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        assertTrue(json.contains("\"intel_id\""), json);
        assertTrue(json.contains("\"key_points\""), json);
        assertFalse(json.contains("keyPoints"), "不得出现 camelCase：" + json);
        // key_points JSON 列元素原样下发
        assertTrue(json.contains("\"面向中低速率场景\""), json);
        assertTrue(json.contains("\"2026-07-11 10:00:00\""), json);
        assertFalse(json.contains("2026-07-11T10:00"), "禁 ISO 'T' 分隔：" + json);
    }
}
