package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.AuditLogConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.AuditLogDao;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditLogPageDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 审计日志 service 行为测试（此前零覆盖模块）：
 * 覆盖 ①非 admin 一律 FORBIDDEN 且不查库；②pageSize 超限被 Math.min 夹到 500、
 * null 兜底 20；③keyword/actionType/result 三个过滤开关按入参有无正确开合。
 *
 * <p>dao 全 mock；lambdaQuery 链用 {@code Answers.RETURNS_SELF} 让链式方法自返，
 * 仅 page 显式打桩返回空页。链交互 verify 即可，真实 WHERE 拼装由集成测试覆盖。
 * 离线可跑，无需 MyBatis 容器（链对象本身被 mock，不触发 TableInfo 解析）。</p>
 */
class AuditLogServiceImplTest {

    @Mock AuditLogDao dao;
    @Mock CurrentUserResolver currentUser;

    private AuditLogServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new AuditLogServiceImpl(dao, currentUser, new AuditLogConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    /**
     * 造一个自返链，dao.lambdaQuery() 返它；page(any()) 返空页。
     * 返回链对象供调用方 verify 链上交互。
     */
    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<AuditLogDO> stubChain() {
        LambdaQueryChainWrapper<AuditLogDO> chain =
                mock(LambdaQueryChainWrapper.class, Answers.RETURNS_SELF);
        when(dao.lambdaQuery()).thenReturn(chain);
        // page 返回类型是 IPage，与链自身类型不兼容，RETURNS_SELF 兜不住，须显式打桩
        when(chain.page(any())).thenReturn(new Page<AuditLogDO>());
        return chain;
    }

    // ---------- 鉴权：非 admin 一律拒绝且不查库 ----------

    @Test
    void page_denied_for_non_admin_and_never_queries() {
        when(currentUser.requireAnyRole(any()))
                .thenThrow(new BaseException(ErrorCode.FORBIDDEN, "拒绝"));

        assertThrows(BaseException.class, () -> service.page(new AuditLogPageDTO()));
        // 鉴权先于查库，dao 零交互
        verifyNoInteractions(dao);
    }

    // ---------- pageSize 边界：Math.min 夹到 500 ----------

    @Test
    void pageSize_over_limit_is_clamped_to_500() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        LambdaQueryChainWrapper<AuditLogDO> chain = stubChain();

        AuditLogPageDTO dto = new AuditLogPageDTO();
        dto.setPageNumber(2);
        dto.setPageSize(999);
        service.page(dto);

        // 捕获交给 chain.page 的 Page，验证 size 已被夹到 500、页码透传
        ArgumentCaptor<IPage<AuditLogDO>> cap = ArgumentCaptor.forClass(IPage.class);
        verify(chain).page(cap.capture());
        assertEquals(500L, cap.getValue().getSize());
        assertEquals(2L, cap.getValue().getCurrent());
    }

    @Test
    void pageSize_null_defaults_to_20() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        LambdaQueryChainWrapper<AuditLogDO> chain = stubChain();

        // pageNumber/pageSize 皆空，走 service 兜底 1/20
        service.page(new AuditLogPageDTO());

        ArgumentCaptor<IPage<AuditLogDO>> cap = ArgumentCaptor.forClass(IPage.class);
        verify(chain).page(cap.capture());
        assertEquals(20L, cap.getValue().getSize());
        assertEquals(1L, cap.getValue().getCurrent());
    }

    // ---------- 过滤开关：三个条件按入参有无正确开合 ----------

    @Test
    void filters_all_present_turn_conditions_on() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        LambdaQueryChainWrapper<AuditLogDO> chain = stubChain();

        AuditLogPageDTO dto = new AuditLogPageDTO();
        dto.setKeyword("张三");
        dto.setActionType("publish");
        dto.setResult("success");
        service.page(dto);

        // keyword 有值 → and 条件启用
        verify(chain).and(eq(true), any());
        // actionType/result 有值 → 对应 eq 条件启用（按 value 区分两次 eq 调用）
        verify(chain).eq(eq(true), any(), eq("publish"));
        verify(chain).eq(eq(true), any(), eq("success"));
    }

    @Test
    void filters_all_absent_turn_conditions_off() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        LambdaQueryChainWrapper<AuditLogDO> chain = stubChain();

        // 三个过滤字段全空
        service.page(new AuditLogPageDTO());

        verify(chain).and(eq(false), any());
        // actionType 与 result 皆空 → 两次 eq 均以 false 关闭、值为 null
        verify(chain, times(2)).eq(eq(false), any(), isNull());
    }

    private com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO admin() {
        com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO u =
                new com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO();
        u.setId(9001L);
        u.setName("运营管理员");
        u.setRole(CurrentUserResolver.ROLE_ADMIN);
        u.setStatus("active");
        return u;
    }
}
