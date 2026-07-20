package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.InteractionConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.InteractionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionCommentDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionLikeDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.InteractionServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 互动 service 行为测试：点赞幂等切换、评论 2 级约束、目标存在性、计数原子回写。
 * dao 全 mock，离线可跑（真库行为由 InteractionIntegrationTest 覆盖）。
 */
class InteractionServiceImplTest {

    @Mock InteractionDao dao;
    @Mock OpportunityDao opportunityDao;
    @Mock RequirementDao requirementDao;
    @Mock SolutionResponseDao solutionResponseDao;
    @Mock SysUserDao sysUserDao;
    @Mock CurrentUserResolver currentUser;

    private InteractionServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new InteractionServiceImpl(dao, opportunityDao, requirementDao,
                solutionResponseDao, sysUserDao, currentUser, new InteractionConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO me() {
        SysUserDO u = new SysUserDO();
        u.setId(9002L);
        u.setName("张伟");
        u.setStatus("active");
        u.setDepartmentName("上海销售组");
        return u;
    }

    private OpportunityDO existingOpportunity() {
        OpportunityDO d = new OpportunityDO();
        d.setId(1001L);
        return d;
    }

    private InteractionLikeDTO likeDto(String id, String targetType, String type) {
        InteractionLikeDTO dto = new InteractionLikeDTO();
        dto.setId(id);
        dto.setTargetType(targetType);
        dto.setType(type);
        return dto;
    }

    private InteractionCommentDTO commentDto(String targetId, String content, String parentId) {
        InteractionCommentDTO dto = new InteractionCommentDTO();
        dto.setTargetType("Opportunity");
        dto.setTargetId(targetId);
        dto.setContent(content);
        dto.setParentId(parentId);
        return dto;
    }

    // ---------- like 幂等切换 ----------

    @Test
    void repeat_like_triggers_duplicate_then_cancels_and_decrements_atomically() {
        when(currentUser.currentOrNull()).thenReturn(me());
        when(opportunityDao.getById(1001L)).thenReturn(existingOpportunity());
        // 唯一键命中：insert 抛 DuplicateKeyException → 取消路径
        when(dao.save(any())).thenThrow(new DuplicateKeyException("uk_inter_reaction"));

        com.quectel.web.cloud.salesleadhubserver.vo.ReactionVO result =
                service.like(likeDto("1001", "Opportunity", "like"));

        assertFalse(result.isLiked(), "重复点赞应切换为取消");
        // 取消 = 按 reaction 维度物理删该行
        verify(dao).remove(any());
        // 计数原子自减：GREATEST(like_count - 1, 0)，走 setSql，不是读出+写回
        ArgumentCaptor<LambdaUpdateWrapper> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(opportunityDao).update(cap.capture());
        String setSql = cap.getValue().getSqlSet();
        assertTrue(setSql.contains("like_count"), setSql);
        assertTrue(setSql.contains("- 1"), setSql);
        assertTrue(setSql.contains("GREATEST"), setSql);
    }

    @Test
    void like_on_missing_target_is_rejected_and_never_saves() {
        when(currentUser.currentOrNull()).thenReturn(me());
        when(opportunityDao.getById(1001L)).thenReturn(null);   // 目标不存在

        assertThrows(BaseException.class, () -> service.like(likeDto("1001", "Opportunity", "like")));
        verify(dao, never()).save(any());
    }

    // ---------- comment 2 级约束 ----------

    @Test
    void three_level_reply_is_rejected() {
        when(currentUser.currentOrNull()).thenReturn(me());
        when(opportunityDao.getById(1001L)).thenReturn(existingOpportunity());
        // 父指向的是一条二级回复（自身 parentCommentId 非空）→ 三级，应拒
        InteractionDO reply = new InteractionDO();
        reply.setId(2002L);
        reply.setType("comment");
        reply.setParentCommentId(2001L);
        reply.setTargetType("Opportunity");
        reply.setTargetId(1001L);
        when(dao.getById(2002L)).thenReturn(reply);

        assertThrows(BaseException.class,
                () -> service.comment(commentDto("1001", "三级回复", "2002")));
        verify(dao, never()).save(any());
    }

    @Test
    void reply_to_first_level_comment_is_accepted_and_bumps_comment_count() {
        when(currentUser.currentOrNull()).thenReturn(me());
        when(opportunityDao.getById(1001L)).thenReturn(existingOpportunity());
        InteractionDO firstLevel = new InteractionDO();
        firstLevel.setId(2001L);
        firstLevel.setType("comment");
        firstLevel.setParentCommentId(null);   // 一级评论
        firstLevel.setTargetType("Opportunity");
        firstLevel.setTargetId(1001L);
        when(dao.getById(2001L)).thenReturn(firstLevel);

        service.comment(commentDto("1001", "二级回复", "2001"));

        verify(dao).save(any());
        // comment_count 原子自增
        ArgumentCaptor<LambdaUpdateWrapper> cap = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(opportunityDao).update(cap.capture());
        String setSql = cap.getValue().getSqlSet();
        assertTrue(setSql.contains("comment_count"), setSql);
        assertTrue(setSql.contains("+ 1"), setSql);
    }

    @Test
    void comment_on_missing_target_is_rejected() {
        when(currentUser.currentOrNull()).thenReturn(me());
        when(opportunityDao.getById(1001L)).thenReturn(null);

        assertThrows(BaseException.class,
                () -> service.comment(commentDto("1001", "评论内容", null)));
        verify(dao, never()).save(any());
    }

    @Test
    void blank_comment_content_is_rejected() {
        when(currentUser.currentOrNull()).thenReturn(me());

        assertThrows(BaseException.class,
                () -> service.comment(commentDto("1001", "   ", null)));
        verify(dao, never()).save(any());
    }

    @Test
    void comment_by_unopened_account_is_forbidden() {
        when(currentUser.currentOrNull()).thenReturn(null);   // 未开通本地档案

        assertThrows(BaseException.class,
                () -> service.comment(commentDto("1001", "内容", null)));
        verify(dao, never()).save(any());
    }
}
