package com.quectel.web.cloud.salesleadhubserver.integration;

import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 需 MySQL 可达的集成测试。
 *
 * <p>类名必须以 {@code Test} 结尾——surefire 默认不扫描 {@code *IT}，用 IT 命名会导致
 * 这个类根本不被执行却报 BUILD SUCCESS。{@code @Tag("integration")} +
 * pom 的 excludedGroups 使其默认不进离线门禁。</p>
 *
 * <p>反选执行：{@code mvn test -Dtest=RequirementIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，且 <b>Tests run: 0 视为 FAIL</b>。</p>
 */
@Tag("integration")
@SpringBootTest
class RequirementIntegrationTest {

    /** data.sql 的销售账号张伟（sys_user.id=9002）。 */
    private static final Long SEED_USER_ID = 9002L;

    @Autowired
    RequirementDao dao;

    /** 黑盒② 乐观锁真实生效 + 黑盒⑤ List&lt;String&gt; 的 JacksonTypeHandler 往返。 */
    @Test
    void optimistic_lock_stale_replay_and_json_roundtrip() {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setTitle("并发测试");
        d.setDescription("x");
        d.setIndustry("IoT");
        d.setUrgency("normal");
        d.setStatus("Pending");
        d.setVisibilityScope("all");
        // publisher_id 是 NOT NULL 无默认值。正常链路由 service 从 SSO 上下文回填，
        // 本测试直连 dao、绕开了 service，故手工给一个 data.sql 里存在的 sys_user id。
        d.setPublisherId(SEED_USER_ID);
        d.setCategoryNames(Arrays.asList("5G 模组", "NB-IoT 模组"));
        dao.save(d);

        Long id = d.getId();
        assertNotNull(id, "雪花 id 应回填");

        OpportunityRequestDO a = dao.getById(id);
        OpportunityRequestDO b = dao.getById(id);          // 两份持有同一 version

        assertEquals(Arrays.asList("5G 模组", "NB-IoT 模组"), a.getCategoryNames(),
                "⑤ List<String> 经 JacksonTypeHandler 往返应一致");
        Integer v0 = a.getVersion();
        assertNotNull(v0, "version 应有初值");

        a.setTitle("先改");
        assertTrue(dao.updateById(a), "首次更新应成功");
        assertEquals(Integer.valueOf(v0 + 1), dao.getById(id).getVersion(), "② version 应自增");

        b.setTitle("后改(陈旧 version)");
        assertFalse(dao.updateById(b),
                "② 陈旧 version 应影响 0 行 → false；若为 true 说明 Task 1 的拦截器覆盖未生效");
    }
}
