package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.ProfileConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.InteractionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SubscriptionDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.ViewLogMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.ProfileServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCenterVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 个人中心 service 行为测试：只返回本人（无本地档案 fail-closed）、九段组装齐备、不泄漏敏感字段。
 *
 * <p>DAO 的 fluent 链用 {@link Answers#RETURNS_SELF} 桩返回空结果，重点验证「组装 + 脱敏 + 段齐备」；
 * 五段列表的逐行映射（回查/跳删/键名）由 {@code ProfileConvertTest} 与真库集成覆盖。</p>
 */
class ProfileServiceImplTest {

    @Mock CurrentUserResolver currentUser;
    @Mock OpportunityDao opportunityDao;
    @Mock RequirementDao requirementDao;
    @Mock SubscriptionDao subscriptionDao;
    @Mock CategoryDao categoryDao;
    @Mock InteractionDao interactionDao;
    @Mock SolutionResponseDao solutionResponseDao;
    @Mock ViewLogMapper viewLogMapper;

    private ProfileServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new ProfileServiceImpl(currentUser, opportunityDao, requirementDao,
                subscriptionDao, categoryDao, interactionDao, solutionResponseDao,
                viewLogMapper, new ProfileConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO me() {
        SysUserDO u = new SysUserDO();
        u.setId(10160L);
        u.setName("张伟");
        u.setRole(CurrentUserResolver.ROLE_PRODUCT_MANAGER);
        u.setDepartmentName("无线模组产品部");
        u.setEmployeeId("QT100237");
        u.setPhone("13800001234");   // 敏感字段：断言绝不出现在出参
        u.setStatus("active");
        return u;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<Object> emptyChain(long count) {
        LambdaQueryChainWrapper<Object> chain =
                mock(LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        when(chain.list()).thenReturn(Collections.emptyList());
        when(chain.count()).thenReturn(count);
        return chain;
    }

    @Test
    void center_without_local_profile_is_forbidden() {
        when(currentUser.currentOrNull()).thenReturn(null);
        assertThrows(BaseException.class, () -> service.center());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void center_assembles_all_segments_and_never_leaks_phone() throws Exception {
        when(currentUser.currentOrNull()).thenReturn(me());
        // 各 DAO 的 fluent 链：list 空、count 给定值（同一 DAO 的多次 count 复用同值）。
        // ⚠️ 链 mock 必须先构建再 stub——写成 thenReturn(emptyChain(...)) 会在外层 stubbing
        // 未收口时嵌套开启新 stubbing，抛 UnfinishedStubbingException。
        LambdaQueryChainWrapper interChain = emptyChain(5L);    // collect/comment=5
        LambdaQueryChainWrapper oppChain = emptyChain(3L);      // published/draft=3
        LambdaQueryChainWrapper reqChain = emptyChain(2L);      // 我的需求=2
        LambdaQueryChainWrapper solChain = emptyChain(1L);      // 我的方案=1
        LambdaQueryChainWrapper catChain = emptyChain(0L);
        LambdaQueryChainWrapper subChain = emptyChain(0L);
        when(interactionDao.lambdaQuery()).thenReturn(interChain);
        when(opportunityDao.lambdaQuery()).thenReturn(oppChain);
        when(requirementDao.lambdaQuery()).thenReturn(reqChain);
        when(solutionResponseDao.lambdaQuery()).thenReturn(solChain);
        when(categoryDao.lambdaQuery()).thenReturn(catChain);
        when(subscriptionDao.lambdaQuery()).thenReturn(subChain);
        when(viewLogMapper.selectCount(any())).thenReturn(7L);
        when(viewLogMapper.selectList(any())).thenReturn(Collections.emptyList());

        ProfileCenterVO vo = service.center();

        // 用户段：角色码转中文名
        assertEquals("张伟", vo.getUser().getName());
        assertEquals("产品经理", vo.getUser().getRoleName());
        assertEquals("无线模组产品部", vo.getUser().getDeptName());
        assertEquals("QT100237", vo.getUser().getEmployeeNo());

        // 统计段：publish=opp(3)+req(2)=5，draft=3，collect/comment=5，solution=1，view=7
        assertEquals(Integer.valueOf(5), vo.getStats().getPublishCount());
        assertEquals(Integer.valueOf(3), vo.getStats().getDraftCount());
        assertEquals(Integer.valueOf(5), vo.getStats().getCollectCount());
        assertEquals(Integer.valueOf(1), vo.getStats().getSolutionCount());
        assertEquals(Integer.valueOf(7), vo.getStats().getViewCount());

        // 九段齐备（列表段空但非 null，避免前端拿到 undefined）
        assertNotNull(vo.getSubscriptionTree());
        assertNotNull(vo.getSubscribedKeys());
        assertNotNull(vo.getCollects());
        assertNotNull(vo.getPublishes());
        assertNotNull(vo.getSolutions());
        assertNotNull(vo.getComments());
        assertNotNull(vo.getViewHistory());

        // 脱敏 + 契约：序列化后绝不出现 phone / 手机号，且键名 snake_case
        String json = new ObjectMapper().writeValueAsString(vo);
        assertFalse(json.contains("phone"), "不得泄漏 phone：" + json);
        assertFalse(json.contains("13800001234"), "不得泄漏手机号：" + json);
        assertTrue(json.contains("\"subscription_tree\""), json);
        assertTrue(json.contains("\"subscribed_keys\""), json);
        assertTrue(json.contains("\"view_history\""), json);
        assertTrue(json.contains("\"role_name\""), json);
        assertTrue(json.contains("\"employee_no\""), json);
        assertTrue(json.contains("\"collect_count\""), json);
        assertFalse(json.contains("roleName"), "不得出现 camelCase：" + json);
    }
}
