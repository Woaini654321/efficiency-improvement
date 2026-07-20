package com.quectel.web.cloud.salesleadhubserver.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionPostDao;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionReplyDao;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionPageDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionReplyDO;
import com.quectel.web.cloud.salesleadhubserver.service.DiscussionService;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionPageVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 讨论区真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见
 * RequirementIntegrationTest）。测试数据自建自删，不依赖种子。
 *
 * <p>反选执行：{@code mvn test -Dtest=DiscussionIntegrationTest -Dgroups=integration
 * -Dsurefire.excludedGroups=}，Tests run: 0 视为 FAIL。</p>
 *
 * <p>create 端点走 {@link DiscussionService} 会要求 SSO 登录态（requireAnyRole），
 * 集成测试无登录上下文，故建帖直接用 dao.save（NOT NULL 的 author_id 手工给），
 * 与 OpportunityIntegrationTest 同思路；page/detail 不校角色，可直接走 service。</p>
 */
@Tag("integration")
@SpringBootTest
class DiscussionIntegrationTest {

    /** data.sql 的销售账号张伟。 */
    private static final Long SEED_USER_ID = 9002L;

    @Autowired
    DiscussionPostDao postDao;

    @Autowired
    DiscussionReplyDao replyDao;

    @Autowired
    DiscussionService service;

    /**
     * 建帖 → page 可见（关键词+话题过滤命中）→ detail 浏览数原子 +1 → 多层回帖树组装正确。
     */
    @Test
    void post_visible_view_count_increments_and_reply_tree_assembles() {
        String uniqueTitle = "集测讨论帖-" + System.nanoTime();
        DiscussionPostDO d = new DiscussionPostDO();
        d.setTitle(uniqueTitle);
        d.setContent("RedCap 选型集成测试正文");
        d.setTopic("business");
        d.setAuthorId(SEED_USER_ID);      // NOT NULL 列，直连 dao 绕开 service 须手工给
        d.setAuthorName("张伟");
        d.setTags(Arrays.asList("5G", "集测"));
        d.setReplyCount(0);
        d.setViewCount(0);
        d.setIsHot(false);
        postDao.save(d);

        Long postId = d.getId();
        assertNotNull(postId, "雪花 id 应回填");

        List<Long> replyIds = new ArrayList<>();
        try {
            // tags（List<String>）经 JacksonTypeHandler 往返应结构一致
            DiscussionPostDO loaded = postDao.getById(postId);
            assertEquals(Arrays.asList("5G", "集测"), loaded.getTags());

            // 构造 3 层回帖树：root(10:00) → child(10:20) → grandchild(10:35)
            Long rootId = saveReply(postId, null, "李娜", LocalDateTime.of(2026, 7, 10, 10, 0, 0), replyIds);
            Long childId = saveReply(postId, rootId, "张伟", LocalDateTime.of(2026, 7, 10, 10, 20, 0), replyIds);
            saveReply(postId, childId, "王强", LocalDateTime.of(2026, 7, 10, 10, 35, 0), replyIds);

            // page 可见：关键词命中标题 + 话题精确过滤
            DiscussionPageDTO pageDto = new DiscussionPageDTO();
            pageDto.setPageNumber(1);
            pageDto.setPageSize(50);
            pageDto.setKeyword(uniqueTitle);
            pageDto.setTopic("business");
            boolean visible = service.page(pageDto).getRecords().stream()
                    .map(DiscussionPageVO::getPostId)
                    .anyMatch(postId::equals);
            assertTrue(visible, "发布后应能在列表按关键词+话题查到");

            // detail：浏览数原子 +1（0→1），回帖树组装
            DiscussionDetailVO vo = service.detail(postId);
            assertEquals(Integer.valueOf(1), vo.getViewCount(), "view_count 应原子自增到 1");
            assertEquals(Integer.valueOf(1), postDao.getById(postId).getViewCount(),
                    "库里 view_count 也应为 1");

            assertEquals(1, vo.getComments().size(), "一个根回帖");
            DiscussionCommentVO root = vo.getComments().get(0);
            assertEquals(rootId, root.getCommentId());
            assertEquals(1, root.getChildren().size(), "root 下一层");
            DiscussionCommentVO child = root.getChildren().get(0);
            assertEquals(childId, child.getCommentId());
            assertEquals(1, child.getChildren().size(), "深度 ≥3 层（不受 ≤2 级约束）");
        } finally {
            if (!replyIds.isEmpty()) {
                replyDao.remove(new LambdaQueryWrapper<DiscussionReplyDO>()
                        .eq(DiscussionReplyDO::getPostId, postId));
            }
            postDao.removeById(postId);
        }
    }

    private Long saveReply(Long postId, Long parentId, String author, LocalDateTime time, List<Long> sink) {
        DiscussionReplyDO r = new DiscussionReplyDO();
        r.setPostId(postId);
        r.setParentId(parentId);
        r.setAuthorId(SEED_USER_ID);
        r.setAuthorName(author);
        r.setContent("集测回帖-" + author);
        r.setCreateTime(time);
        replyDao.save(r);
        sink.add(r.getId());
        return r.getId();
    }
}
