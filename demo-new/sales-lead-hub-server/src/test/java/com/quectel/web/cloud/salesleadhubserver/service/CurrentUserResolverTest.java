package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 业务角色解析的安全边界测试。
 *
 * <p>这些是 fail-closed 断言：账号未开通 / 已停用 / 角色不匹配，一律拒绝。
 * 任何一条被改成放行都是权限漏洞，故单独成类钉死。</p>
 */
class CurrentUserResolverTest {

    @Mock SysUserDao sysUserDao;
    @InjectMocks CurrentUserResolver resolver;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(String role, String status) {
        SysUserDO u = new SysUserDO();
        u.setId(10160L);
        u.setUsername("atom.ye");
        u.setName("atom.ye");
        u.setRole(role);
        u.setStatus(status);
        return u;
    }

    @Test
    void returns_profile_when_role_matches() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(10160L);
            when(sysUserDao.getById(10160L)).thenReturn(user(CurrentUserResolver.ROLE_SALES, "active"));

            SysUserDO me = resolver.requireAnyRole(
                    CurrentUserResolver.ROLE_SALES, CurrentUserResolver.ROLE_ADMIN);

            assertEquals(Long.valueOf(10160L), me.getId());
        }
    }

    @Test
    void rejects_when_not_provisioned_locally() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(10160L);
            when(sysUserDao.getById(10160L)).thenReturn(null);   // UAA 能登录，但本平台没开通

            BaseException ex = assertThrows(BaseException.class,
                    () -> resolver.requireAnyRole(CurrentUserResolver.ROLE_SALES));
            assertTrue(ex.getMessage().contains("尚未在本平台开通"), ex.getMessage());
        }
    }

    @Test
    void rejects_disabled_account() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(10160L);
            when(sysUserDao.getById(10160L)).thenReturn(user(CurrentUserResolver.ROLE_SALES, "disabled"));

            BaseException ex = assertThrows(BaseException.class,
                    () -> resolver.requireAnyRole(CurrentUserResolver.ROLE_SALES));
            assertTrue(ex.getMessage().contains("已停用"), ex.getMessage());
        }
    }

    @Test
    void rejects_when_role_not_in_allowed_list() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(10160L);
            when(sysUserDao.getById(10160L))
                    .thenReturn(user(CurrentUserResolver.ROLE_PRODUCT_MANAGER, "active"));

            assertThrows(BaseException.class,
                    () -> resolver.requireAnyRole(CurrentUserResolver.ROLE_ADMIN));
        }
    }

    @Test
    void currentOrNull_returns_null_without_login() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(null);
            assertNull(resolver.currentOrNull(), "取不到 userId 时不得查库、不得抛异常");
        }
    }
}
