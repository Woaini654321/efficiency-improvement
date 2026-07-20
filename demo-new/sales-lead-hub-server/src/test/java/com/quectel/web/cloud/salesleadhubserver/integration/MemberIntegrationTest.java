package com.quectel.web.cloud.salesleadhubserver.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 产品线成员模块真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见
 * RequirementIntegrationTest）。DAO 直连，验证 DB 级约束与快照往返；owner 唯一的
 * <b>应用层规则</b>由 MemberServiceImplTest 覆盖，本测试钉死其依赖的 DB 数据条件。
 *
 * <p>沙箱产品线用保留 id 89901（不存在于任何种子；本表无物理外键，成员行可独立存在），
 * @AfterEach 按该 id 清理。⚠️ 不要改用真实种子产品线（501~505）：demo 种子会给它们配
 * owner，本测试的 cleanup 也会误删种子行——2026-07-20 在 505 上实际踩过。</p>
 *
 * <p>反选执行：{@code mvn test -Dtest=MemberIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，Tests run: 0 视为 FAIL。</p>
 */
@Tag("integration")
@SpringBootTest
class MemberIntegrationTest {

    /** 保留沙箱产品线 id（无种子引用；见类 javadoc 警告）。 */
    private static final Long SANDBOX_LINE_ID = 89901L;
    /** data.sql 的销售账号张伟 / 产品经理李娜。 */
    private static final Long USER_ZHANGWEI = 9002L;
    private static final Long USER_LINA = 9003L;

    @Autowired
    ProductLineMemberDao memberDao;

    @AfterEach
    void cleanup() {
        memberDao.remove(new LambdaQueryWrapper<ProductLineMemberDO>()
                .eq(ProductLineMemberDO::getProductLineId, SANDBOX_LINE_ID));
    }

    @Test
    void add_page_owner_uniqueness_and_duplicate_key() {
        // add owner 成员
        ProductLineMemberDO owner = member(USER_ZHANGWEI, "张伟", 1);
        memberDao.save(owner);
        assertNotNull(owner.getId(), "雪花 id 应回填");

        // getById 快照往返
        ProductLineMemberDO loaded = memberDao.getById(owner.getId());
        assertEquals("张伟", loaded.getUserName());
        assertEquals(Integer.valueOf(1), loaded.getIsOwner());
        assertNotNull(loaded.getCreateTime(), "create_time 应由框架自动填充");

        // page：按产品线过滤应能查到这一条
        long count = memberDao.count(new LambdaQueryWrapper<ProductLineMemberDO>()
                .eq(ProductLineMemberDO::getProductLineId, SANDBOX_LINE_ID));
        assertEquals(1L, count);

        // 重复添加同一产品线同一人 → 命中 uk_plm_line_user
        ProductLineMemberDO dup = member(USER_ZHANGWEI, "张伟", 0);
        assertThrows(DuplicateKeyException.class, () -> memberDao.save(dup),
                "uk_plm_line_user 应拒绝同产品线重复成员");

        // add 第二个非 owner 成员（不同用户）应成功
        ProductLineMemberDO second = member(USER_LINA, "李娜", 0);
        memberDao.save(second);

        // owner 唯一：该产品线当前恰好 1 个 owner
        long owners = memberDao.count(new LambdaQueryWrapper<ProductLineMemberDO>()
                .eq(ProductLineMemberDO::getProductLineId, SANDBOX_LINE_ID)
                .eq(ProductLineMemberDO::getIsOwner, 1));
        assertEquals(1L, owners, "一条产品线至多一个 owner");

        // 「把第二个成员升为 owner」的冲突数据条件：排除其自身后，仍存在其他 owner
        boolean otherOwnerExists = memberDao.lambdaQuery()
                .eq(ProductLineMemberDO::getProductLineId, SANDBOX_LINE_ID)
                .eq(ProductLineMemberDO::getIsOwner, 1)
                .ne(ProductLineMemberDO::getId, second.getId())
                .exists();
        assertTrue(otherOwnerExists, "升 owner 前应检测到已有其他 owner —— service 据此抛 OWNER_CONFLICT");
    }

    private ProductLineMemberDO member(Long userId, String userName, int isOwner) {
        ProductLineMemberDO d = new ProductLineMemberDO();
        d.setProductLineId(SANDBOX_LINE_ID);
        d.setUserId(userId);
        d.setUserName(userName);
        d.setIsOwner(isOwner);
        return d;
    }
}
