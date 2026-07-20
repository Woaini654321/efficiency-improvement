package com.quectel.web.cloud.salesleadhubserver.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.dao.InteractionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionCommentDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionLikeDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.service.InteractionService;
import com.quectel.web.cloud.salesleadhubserver.vo.ReactionVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

/**
 * 互动模块真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见
 * OpportunityIntegrationTest）。
 *
 * <p>点赞/评论需当前登录人身份，用 {@code MockedStatic<SecurityUtils>} 注入 data.sql 的
 * 销售账号张伟（id=9002）；商机与互动行测试内自建自删。</p>
 */
@Tag("integration")
@SpringBootTest
class InteractionIntegrationTest {

    /** data.sql 的销售账号张伟（sys_user.id=9002）。 */
    private static final Long SEED_USER_ID = 9002L;

    @Autowired
    InteractionService service;

    @Autowired
    InteractionDao interactionDao;

    @Autowired
    OpportunityDao opportunityDao;

    /** like → 行存在 + like_count+1 → 再 like → 行删除 + 计数-1（幂等切换 + 原子回写）。 */
    @Test
    void like_toggles_row_and_writes_back_count() {
        Long oppId = createOpportunity("互动集成-点赞");
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(SEED_USER_ID);

            InteractionLikeDTO dto = new InteractionLikeDTO();
            dto.setId(String.valueOf(oppId));
            dto.setTargetType("Opportunity");
            dto.setType("like");

            // 首次点赞：行存在 + 计数 1
            ReactionVO r1 = service.like(dto);
            assertTrue(r1.isLiked(), "首次应为已点赞");
            assertEquals(1L, countReaction(oppId, "like"), "reaction 行应存在");
            assertEquals(Integer.valueOf(1), opportunityDao.getById(oppId).getLikeCount(),
                    "like_count 应回写为 1");

            // 再次点赞：行删除 + 计数回到 0
            ReactionVO r2 = service.like(dto);
            assertFalse(r2.isLiked(), "再次点赞应取消");
            assertEquals(0L, countReaction(oppId, "like"), "取消后 reaction 行应物理删除");
            assertEquals(Integer.valueOf(0), opportunityDao.getById(oppId).getLikeCount(),
                    "like_count 应回写为 0（不出负数）");
        } finally {
            cleanup(oppId);
        }
    }

    /** 两级评论插入成功、三级被拒；comment_count 逐次回写。 */
    @Test
    void two_level_comment_ok_and_three_level_rejected() {
        Long oppId = createOpportunity("互动集成-评论");
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(SEED_USER_ID);

            Long c1 = service.comment(comment(oppId, "一级评论", null));
            assertNotNull(c1);
            Long c2 = service.comment(comment(oppId, "二级回复", String.valueOf(c1)));
            assertNotNull(c2, "回复一级评论应成功");
            assertEquals(Integer.valueOf(2), opportunityDao.getById(oppId).getCommentCount(),
                    "两条评论后 comment_count 应为 2");

            // 回复一条二级回复 = 三级，应被拒
            assertThrows(BaseException.class,
                    () -> service.comment(comment(oppId, "三级回复", String.valueOf(c2))),
                    "评论至多 2 级，三级须被拒");
            // 被拒不应改变计数
            assertEquals(Integer.valueOf(2), opportunityDao.getById(oppId).getCommentCount());
        } finally {
            cleanup(oppId);
        }
    }

    // ---------- helpers ----------

    private Long createOpportunity(String title) {
        OpportunityDO d = new OpportunityDO();
        d.setTitle(title);
        d.setType("solution");
        d.setStatus("published");
        d.setPublisherId(SEED_USER_ID);   // NOT NULL，直连 dao 绕开 service 须手工给
        d.setViewCount(0);
        d.setLikeCount(0);
        d.setCollectCount(0);
        d.setCommentCount(0);
        opportunityDao.save(d);
        return d.getId();
    }

    private InteractionCommentDTO comment(Long oppId, String content, String parentId) {
        InteractionCommentDTO dto = new InteractionCommentDTO();
        dto.setTargetType("Opportunity");
        dto.setTargetId(String.valueOf(oppId));
        dto.setContent(content);
        dto.setParentId(parentId);
        return dto;
    }

    private long countReaction(Long oppId, String type) {
        return interactionDao.count(Wrappers.<InteractionDO>lambdaQuery()
                .eq(InteractionDO::getTargetType, "Opportunity")
                .eq(InteractionDO::getTargetId, oppId)
                .eq(InteractionDO::getType, type));
    }

    private void cleanup(Long oppId) {
        // 互动行物理删（评论 + 点赞 reaction 均 targetType=Opportunity、targetId=oppId）
        interactionDao.remove(new LambdaQueryWrapper<InteractionDO>()
                .eq(InteractionDO::getTargetType, "Opportunity")
                .eq(InteractionDO::getTargetId, oppId));
        // 商机逻辑删（框架 deleted），不干扰后续
        opportunityDao.removeById(oppId);
    }
}
