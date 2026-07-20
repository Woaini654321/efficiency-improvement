package com.quectel.web.cloud.salesleadhubserver.integration;

import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.dao.AnnouncementDao;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnouncementPageDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.service.AnnouncementService;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公告模块真库集成测试（需 MySQL 可达；类名以 Test 结尾 + {@code @Tag("integration")}
 * 隔离，理由见 OpportunityIntegrationTest）。
 *
 * <p>状态迁移用 DAO 直接落库（运营写端点需 admin 安全上下文，集成测试不便构造，与
 * OpportunityIntegrationTest 一致走 DAO 层）；前台可见性/浏览数自增走 service（前台端不校角色，
 * 无需安全上下文），覆盖：
 * create(草稿) → 发布 → 前台可见 + 浏览数原子自增 → 归档 → 前台不可见 → 物理删（无 deleted 列）。</p>
 *
 * <p>反选执行：{@code mvn test -Dtest=AnnounceIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，Tests run: 0 视为 FAIL。</p>
 */
@Tag("integration")
@SpringBootTest
class AnnounceIntegrationTest {

    /** data.sql 的销售账号张伟（sys_user.id=9002），仅用于填 publisher_id NOT NULL 列。 */
    private static final Long SEED_USER_ID = 9002L;

    @Autowired
    AnnouncementDao dao;

    @Autowired
    AnnouncementService service;

    /** 本用例创建的公告 id，供 AfterEach 兜底清理（announcement 无 deleted 列，物理删）。 */
    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        // 兜底：正常路径用例尾部已物理删；此处覆盖「断言中途失败、尾部删未执行」的情形。
        // 对已删/不存在的 id，removeById 影响 0 行返 false，无副作用，null 安全。
        for (Long id : createdIds) {
            if (id != null) {
                dao.removeById(id);
            }
        }
        createdIds.clear();
    }

    @Test
    void full_lifecycle_publish_visible_increment_archive_hidden_physical_delete() {
        String uniqueTitle = "集成公告-" + System.nanoTime();

        // ---- create：草稿落库 ----
        AnnouncementDO d = new AnnouncementDO();
        d.setTitle(uniqueTitle);
        d.setContent("<p>集成测试正文</p>");
        d.setType("notice");
        d.setPriority("normal");
        d.setStatus("draft");
        d.setIsPinned(0);
        d.setBannerEnabled(0);
        d.setViewCount(0);
        d.setPublisherId(SEED_USER_ID);      // NOT NULL 列，直连 dao 绕开 service 须手工给
        d.setPublisherName("张伟");
        dao.save(d);
        Long id = d.getId();
        createdIds.add(id);   // 立即登记，确保后续任一断言失败也能被 AfterEach 清掉
        assertNotNull(id, "雪花 id 应回填");

        // 草稿态：前台详情不可见
        assertThrows(BaseException.class, () -> service.frontDetail(id), "草稿不应对前台可见");

        // ---- 发布：draft → published（补 published_at）----
        AnnouncementDO toPublish = dao.getById(id);
        toPublish.setStatus("published");
        toPublish.setPublishedAt(LocalDateTime.now());
        assertTrue(dao.updateById(toPublish), "发布更新应成功");

        // ---- 前台详情可见 + 浏览数原子自增 ----
        AnnouncementDetailVO detail = service.frontDetail(id);
        assertEquals(uniqueTitle, detail.getTitle());
        assertEquals(Integer.valueOf(1), detail.getViewCount(), "首次详情浏览应 +1");
        assertEquals(Integer.valueOf(1), dao.getById(id).getViewCount(),
                "view_count 必须真的落库自增（setSql 原子自增）");

        // 再看一次，验证是原子累加而非置 1
        service.frontDetail(id);
        assertEquals(Integer.valueOf(2), dao.getById(id).getViewCount(), "第二次详情应累加到 2");

        // ---- 前台列表可见 ----
        assertTrue(frontPageContains(uniqueTitle), "已发布公告应出现在前台列表");

        // ---- 归档：published → archived ----
        AnnouncementDO toArchive = dao.getById(id);
        toArchive.setStatus("archived");
        assertTrue(dao.updateById(toArchive), "归档更新应成功");

        // ---- 前台不可见（详情 + 列表）----
        assertThrows(BaseException.class, () -> service.frontDetail(id), "已归档不应对前台可见");
        assertFalse(frontPageContains(uniqueTitle), "已归档公告不应出现在前台列表");

        // ---- 物理删：announcement 无 deleted 列，removeById 真删行 ----
        assertTrue(dao.removeById(id), "物理删应影响 1 行");
        assertNull(dao.getById(id), "物理删后该行应彻底消失（无逻辑删除隐藏）");
    }

    /** 用唯一标题作 keyword 在前台分页里检索目标行，避免受库中既有数据干扰。 */
    private boolean frontPageContains(String title) {
        AnnouncementPageDTO dto = new AnnouncementPageDTO();
        dto.setPageNumber(1);
        dto.setPageSize(500);
        dto.setKeyword(title);
        PageVO<AnnouncementPageVO> page = service.frontPage(dto);
        return page.getRecords().stream().anyMatch(v -> title.equals(v.getTitle()));
    }
}
