package com.quectel.web.cloud.salesleadhubserver.integration;

import com.quectel.web.cloud.salesleadhubserver.dao.CompetitorIntelDao;
import com.quectel.web.cloud.salesleadhubserver.dao.FeedbackDao;
import com.quectel.web.cloud.salesleadhubserver.dao.IndustryIntelDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.CompetitorIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.FeedbackDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.IndustryIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SpecItem;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 情报中心 + 吐槽墙真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见
 * RequirementIntegrationTest）。测试数据自建自删，不依赖种子。
 *
 * <p>反选执行：{@code mvn test -Dtest=IntelIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，Tests run: 0 视为 FAIL。</p>
 */
@Tag("integration")
@SpringBootTest
class IntelIntegrationTest {

    @Autowired
    CompetitorIntelDao competitorDao;

    @Autowired
    IndustryIntelDao industryDao;

    @Autowired
    FeedbackDao feedbackDao;

    /** 竞品：submit（save）→ page 可见 → specs JSON 往返 → detail view_count 原子 +1。 */
    @Test
    void competitor_submit_visible_and_view_count_increments() {
        CompetitorIntelDO d = new CompetitorIntelDO();
        d.setBrand("集测品牌");
        d.setProduct("集测产品");
        d.setIntelType("new_product");
        d.setTitle("集成测试竞品情报");
        d.setSummary("摘要");
        d.setSource("集测来源");
        d.setSubmitterId(9002L);
        d.setSubmitterName("张伟");
        d.setOverview("<p>概述</p>");
        SpecItem s = new SpecItem();
        s.setLabel("平台");
        s.setValue("高通 SDX62");
        d.setSpecs(Collections.singletonList(s));
        d.setLikeCount(0);
        d.setCollectCount(0);
        d.setViewCount(0);
        competitorDao.save(d);
        Long id = d.getId();
        assertNotNull(id, "雪花 id 应回填");

        try {
            // specs（List<SpecItem>）经 JacksonTypeHandler 往返应结构一致
            CompetitorIntelDO loaded = competitorDao.getById(id);
            assertEquals(1, loaded.getSpecs().size());
            assertEquals("平台", loaded.getSpecs().get(0).getLabel());
            assertEquals("高通 SDX62", loaded.getSpecs().get(0).getValue());

            // page 可见（品牌精确过滤命中）
            boolean visible = competitorDao.lambdaQuery()
                    .eq(CompetitorIntelDO::getBrand, "集测品牌")
                    .list().stream().anyMatch(x -> x.getId().equals(id));
            assertTrue(visible, "提交后应能在竞品列表按品牌查到");

            // detail 浏览数原子 +1
            assertTrue(competitorDao.increaseViewCount(id));
            assertEquals(Integer.valueOf(1), competitorDao.getById(id).getViewCount(),
                    "view_count 应原子自增到 1");
        } finally {
            competitorDao.removeById(id);
        }
    }

    /** 行业：key_points JSON 往返 + detail view_count 原子 +1。 */
    @Test
    void industry_key_points_roundtrip_and_view_count_increments() {
        IndustryIntelDO d = new IndustryIntelDO();
        d.setIndustry("trend");
        d.setTitle("集成测试行业情报");
        d.setSummary("摘要");
        d.setSource("集测来源");
        d.setOverview("<p>概述</p>");
        d.setKeyPoints(Arrays.asList("要点一", "要点二"));
        d.setLikeCount(0);
        d.setCollectCount(0);
        d.setViewCount(0);
        industryDao.save(d);
        Long id = d.getId();
        assertNotNull(id);

        try {
            IndustryIntelDO loaded = industryDao.getById(id);
            assertEquals(Arrays.asList("要点一", "要点二"), loaded.getKeyPoints());

            assertTrue(industryDao.increaseViewCount(id));
            assertEquals(Integer.valueOf(1), industryDao.getById(id).getViewCount());
        } finally {
            industryDao.removeById(id);
        }
    }

    /** 吐槽墙：create（save）→ list 可见 → like_count 原子 +1。 */
    @Test
    void feedback_create_visible_and_like_count_increments() {
        FeedbackDO d = new FeedbackDO();
        d.setTitle("集成测试吐槽");
        d.setContent("内容");
        d.setAnonName("爱吐槽的水獭");
        d.setEmoji("🤯");
        d.setColor("#fa8c16");
        d.setLikeCount(0);
        feedbackDao.save(d);
        Long id = d.getId();
        assertNotNull(id);

        try {
            // list 可见
            boolean visible = feedbackDao.lambdaQuery()
                    .orderByDesc(FeedbackDO::getCreateTime)
                    .list().stream().anyMatch(x -> x.getId().equals(id));
            assertTrue(visible, "发布后应能在吐槽列表查到");

            // 点赞原子 +1
            assertTrue(feedbackDao.increaseLikeCount(id));
            assertEquals(Integer.valueOf(1), feedbackDao.getById(id).getLikeCount(),
                    "like_count 应原子自增到 1");
        } finally {
            feedbackDao.removeById(id);
        }
    }
}
