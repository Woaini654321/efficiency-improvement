package com.quectel.web.cloud.salesleadhubserver.integration;

import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运营审核模块真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见
 * RequirementIntegrationTest）。测试数据自建自删（@AfterEach 逻辑删）。
 *
 * <p>覆盖 service 依赖的三项 DB 落库行为（直连 Dao，绕开需 SSO 上下文的 service）：
 * changePin 后 is_pinned=1 落库、changeStatus 下架后 archived_by 落库、delete 后
 * deleted='Y' 且默认查询不可见。</p>
 *
 * <p>反选执行：{@code mvn test -Dtest=AuditIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，Tests run: 0 视为 FAIL。</p>
 */
@Tag("integration")
@SpringBootTest
class AuditIntegrationTest {

    /** data.sql 的销售账号张伟（sys_user.id=9002）。 */
    private static final Long SEED_USER_ID = 9002L;

    @Autowired
    OpportunityDao opportunityDao;

    @Autowired
    RequirementDao requirementDao;

    private final List<Long> oppIds = new ArrayList<>();
    private final List<Long> reqIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        oppIds.forEach(id -> opportunityDao.removeById(id));
        reqIds.forEach(id -> requirementDao.removeById(id));
    }

    /** changePin：置顶写 is_pinned，两表都要真的落库为 1。 */
    @Test
    void changePin_persists_is_pinned_on_both_tables() {
        OpportunityDO o = newOpportunity("published");
        opportunityDao.save(o);
        oppIds.add(o.getId());

        OpportunityDO loadedO = opportunityDao.getById(o.getId());
        loadedO.setIsPinned(true);
        loadedO.setSortNo(9);
        assertTrue(opportunityDao.updateById(loadedO));
        OpportunityDO afterO = opportunityDao.getById(o.getId());
        assertTrue(afterO.getIsPinned(), "商机 is_pinned 应落库为 1");
        assertNotNull(afterO.getSortNo());

        OpportunityRequestDO r = newRequest("Collecting");
        requirementDao.save(r);
        reqIds.add(r.getId());

        OpportunityRequestDO loadedR = requirementDao.getById(r.getId());
        loadedR.setIsPinned(true);
        assertTrue(requirementDao.updateById(loadedR));
        assertTrue(requirementDao.getById(r.getId()).getIsPinned(), "需求 is_pinned 应落库为 1");
    }

    /** changeStatus 下架：published→archived 且 archived_by 落库为运营 id。 */
    @Test
    void archive_persists_archived_by() {
        OpportunityDO o = newOpportunity("published");
        opportunityDao.save(o);
        oppIds.add(o.getId());

        OpportunityDO loaded = opportunityDao.getById(o.getId());
        loaded.setStatus("archived");
        loaded.setArchivedBy(SEED_USER_ID);
        assertTrue(opportunityDao.updateById(loaded));

        OpportunityDO after = opportunityDao.getById(o.getId());
        assertNotNull(after.getArchivedBy(), "下架后 archived_by 必须落库");
    }

    /** delete：逻辑删后 deleted='Y'，默认查询（框架 @TableLogic）不可见。 */
    @Test
    void delete_logical_hides_row_from_default_query() {
        OpportunityDO o = newOpportunity("published");
        opportunityDao.save(o);
        Long id = o.getId();
        oppIds.add(id);

        assertNotNull(opportunityDao.getById(id), "删除前应可查到");
        assertTrue(opportunityDao.removeById(id), "逻辑删应成功");
        assertNull(opportunityDao.getById(id), "删除后默认查询不可见（deleted='Y'）");
    }

    private OpportunityDO newOpportunity(String status) {
        OpportunityDO d = new OpportunityDO();
        d.setTitle("审核集成-商机-" + System.nanoTime());
        d.setType("solution");
        d.setStatus(status);
        d.setSummary("x");
        d.setPublisherId(SEED_USER_ID);   // NOT NULL 列，直连 dao 须手工给
        d.setIsPinned(false);
        d.setSortNo(0);
        return d;
    }

    private OpportunityRequestDO newRequest(String status) {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setTitle("审核集成-需求-" + System.nanoTime());
        d.setDescription("x");
        d.setIndustry("IoT");
        d.setUrgency("normal");
        d.setStatus(status);
        d.setVisibilityScope("all");
        d.setPublisherId(SEED_USER_ID);
        d.setIsPinned(false);
        d.setSortNo(0);
        return d;
    }
}
