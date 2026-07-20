package com.quectel.web.cloud.salesleadhubserver.integration;

import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.service.SlaCalculator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SLA 真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见 OpportunityIntegrationTest）。
 *
 * <p>自建一条 create_time 落在阈值之外的需求，断言实时派生为 overdue、remaining_text 以「已超时」打头、
 * 升级级数已离开 L0；用后即删（逻辑删）。派生用 {@link SlaCalculator} 对<b>真库往返</b>的行计算，
 * 验证 create_time 经数据库读写后仍能正确驱动实时口径。</p>
 *
 * <p>不直接调 SlaService.page：其入口 requireAnyRole(ADMIN) 依赖 SSO 上下文，集成测试无登录态，
 * 故此处只校验「持久化行 + 派生器」这段，鉴权由 SlaServiceImplTest 覆盖。</p>
 */
@Tag("integration")
@SpringBootTest
class SlaIntegrationTest {

    /** data.sql 的销售账号张伟（sys_user.id=9002）。 */
    private static final Long SEED_USER_ID = 9002L;

    @Autowired
    RequirementDao dao;

    @Autowired
    SlaCalculator calculator;

    @Test
    void overdue_request_roundtrip_derives_overdue_and_remaining_prefix() {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setTitle("SLA集成-首响超时用例");
        d.setUrgency("normal");            // 阈值 24h
        d.setStatus("Pending");
        d.setResponseCount(0);
        d.setPublisherId(SEED_USER_ID);    // NOT NULL 列
        d.setVisibilityScope("all");
        dao.save(d);
        Long id = d.getId();
        assertNotNull(id, "雪花 id 应回填");

        try {
            // create_time 由框架 insert 时填成 now；改到 30h 前使其超过 24h 阈值
            OpportunityRequestDO loaded = dao.getById(id);
            loaded.setCreateTime(LocalDateTime.now().minusHours(30));
            assertTrue(dao.updateById(loaded), "回写 create_time 应成功");

            OpportunityRequestDO reloaded = dao.getById(id);
            LocalDateTime now = LocalDateTime.now();

            assertEquals(SlaCalculator.STATUS_OVERDUE, calculator.deriveStatus(reloaded, now));
            assertTrue(calculator.deriveRemainingText(reloaded, now).startsWith("已超时"),
                    "超时行 remaining_text 应以「已超时」打头");
            assertNotEquals("L0", calculator.deriveEscalationLevel(reloaded, now),
                    "超时行应已升级，不再是 L0");
        } finally {
            dao.removeById(id);   // 逻辑删，清理用例数据
        }
    }
}
