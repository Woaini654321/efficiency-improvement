package com.quectel.web.cloud.salesleadhubserver.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.Attachment;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 商机模块真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见
 * RequirementIntegrationTest）。
 *
 * <p>反选执行：{@code mvn test -Dtest=OpportunityIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，Tests run: 0 视为 FAIL。</p>
 */
@Tag("integration")
@SpringBootTest
class OpportunityIntegrationTest {

    /** data.sql 的销售账号张伟（sys_user.id=9002）。 */
    private static final Long SEED_USER_ID = 9002L;

    @Autowired
    OpportunityDao dao;

    @Autowired
    OpportunityCategoryDao joinDao;

    /** 本用例创建的商机 id，供 AfterEach 兜底清理（商机逻辑删 + 关联表物理删）。 */
    private final List<Long> createdOppIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : createdOppIds) {
            if (id == null) {
                continue;
            }
            // 关联表 opportunity_category 物理删（复合主键，按 opportunityId 批量删，null 安全）
            joinDao.remove(new LambdaQueryWrapper<OpportunityCategoryDO>()
                    .eq(OpportunityCategoryDO::getOpportunityId, id));
            // 商机主表逻辑删（框架 deleted），与 AuditIntegrationTest 一致
            dao.removeById(id);
        }
        createdOppIds.clear();
    }

    /** 乐观锁 + List&lt;Attachment&gt; POJO 的 JacksonTypeHandler 往返 + 关联表全删重写。 */
    @Test
    void optimistic_lock_attachment_roundtrip_and_join_rewrite() {
        OpportunityDO d = new OpportunityDO();
        d.setTitle("集成测试商机");
        d.setType("solution");
        d.setStatus("draft");
        d.setSummary("x");
        d.setPublisherId(SEED_USER_ID);   // NOT NULL 列，直连 dao 绕开 service 须手工给
        d.setCategoryNames(Arrays.asList("5G 模组"));
        Attachment a = new Attachment();
        a.setName("规格书.pdf");
        a.setUrl("https://oss/spec.pdf");
        a.setSize(2048L);
        d.setAttachments(Collections.singletonList(a));
        dao.save(d);

        Long id = d.getId();
        createdOppIds.add(id);   // 立即登记，确保后续断言失败也能被 AfterEach 清掉
        assertNotNull(id, "雪花 id 应回填");

        // List<Attachment> POJO 经 JacksonTypeHandler 往返应结构一致
        OpportunityDO loaded = dao.getById(id);
        assertEquals(1, loaded.getAttachments().size());
        assertEquals("规格书.pdf", loaded.getAttachments().get(0).getName());
        assertEquals(Long.valueOf(2048L), loaded.getAttachments().get(0).getSize());

        // 乐观锁：陈旧 version 重放应失败
        OpportunityDO stale = dao.getById(id);
        loaded.setTitle("先改");
        assertTrue(dao.updateById(loaded), "首次更新应成功");
        stale.setTitle("后改(陈旧)");
        assertFalse(dao.updateById(stale), "陈旧 version 应影响 0 行");

        // 关联表全删重写（复合主键，LambdaQueryWrapper 语义）
        writeJoin(id, Arrays.asList(101L, 10101L));
        assertEquals(Arrays.asList(101L, 10101L), readJoin(id));
        writeJoin(id, Arrays.asList(201L));
        assertEquals(Arrays.asList(201L), readJoin(id), "重写后旧关联行应全部消失");
    }

    /**
     * archived_by 置 null 必须真的落库为 NULL。
     *
     * <p>MP 默认 NOT_NULL 策略会静默跳过 null 字段——实测「恢复上架」后库里
     * 残留旧下架人，单测断言实体全绿但库是错的。该列的
     * {@code updateStrategy = ALWAYS} 就是为此而设，本测试钉死它。</p>
     */
    @Test
    void restore_clears_archived_by_in_database() {
        OpportunityDO d = new OpportunityDO();
        d.setTitle("下架人清空回归");
        d.setType("solution");
        d.setStatus("archived");
        d.setPublisherId(SEED_USER_ID);
        d.setArchivedBy(SEED_USER_ID);
        dao.save(d);
        createdOppIds.add(d.getId());   // 立即登记，供 AfterEach 兜底清理

        OpportunityDO loaded = dao.getById(d.getId());
        loaded.setStatus("published");
        loaded.setArchivedBy(null);
        assertTrue(dao.updateById(loaded));

        assertNull(dao.getById(d.getId()).getArchivedBy(),
                "恢复后 archived_by 必须为 NULL——若残留，说明 ALWAYS 策略被移除");
    }

    private void writeJoin(Long oppId, List<Long> catIds) {
        joinDao.remove(new LambdaQueryWrapper<OpportunityCategoryDO>()
                .eq(OpportunityCategoryDO::getOpportunityId, oppId));
        joinDao.saveBatch(catIds.stream().map(cid -> {
            OpportunityCategoryDO r = new OpportunityCategoryDO();
            r.setOpportunityId(oppId);
            r.setCategoryId(cid);
            return r;
        }).collect(Collectors.toList()));
    }

    private List<Long> readJoin(Long oppId) {
        return joinDao.list(new LambdaQueryWrapper<OpportunityCategoryDO>()
                        .eq(OpportunityCategoryDO::getOpportunityId, oppId)).stream()
                .map(OpportunityCategoryDO::getCategoryId)
                .sorted()
                .collect(Collectors.toList());
    }
}
