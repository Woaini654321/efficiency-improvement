package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.CategoryConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 分类 service：admin 门禁 + 删除双重防呆（子分类/内容引用）。 */
class CategoryServiceImplTest {

    @Mock CategoryDao dao;
    @Mock RequestCategoryDao requestCategoryDao;
    @Mock OpportunityCategoryDao opportunityCategoryDao;
    @Mock CurrentUserResolver currentUser;

    private CategoryServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new CategoryServiceImpl(dao, requestCategoryDao, opportunityCategoryDao,
                currentUser, new CategoryConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO admin() {
        SysUserDO u = new SysUserDO();
        u.setId(9001L);
        u.setRole(CurrentUserResolver.ROLE_ADMIN);
        u.setStatus("active");
        return u;
    }

    @Test
    void create_denied_for_non_admin() {
        when(currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN))
                .thenThrow(new BaseException(com.quectel.code.web.exception.ErrorCode.FORBIDDEN, "拒绝"));
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("x");
        dto.setIsActive(true);
        assertThrows(BaseException.class, () -> service.create(dto));
        verify(dao, never()).save(any());
    }

    @Test
    void delete_blocked_when_has_children() {
        when(currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN)).thenReturn(admin());
        when(dao.getById(1L)).thenReturn(new CategoryDO());
        when(dao.count(any(Wrapper.class))).thenReturn(2L);   // 有子分类

        BaseException ex = assertThrows(BaseException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("子分类"), ex.getMessage());
        verify(dao, never()).removeById(any());
    }

    @Test
    void delete_blocked_when_referenced_by_content() {
        when(currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN)).thenReturn(admin());
        when(dao.getById(1L)).thenReturn(new CategoryDO());
        when(dao.count(any(Wrapper.class))).thenReturn(0L);                       // 无子分类
        when(requestCategoryDao.count(any(Wrapper.class))).thenReturn(3L);        // 需求侧引用
        when(opportunityCategoryDao.count(any(Wrapper.class))).thenReturn(1L);    // 商机侧引用

        BaseException ex = assertThrows(BaseException.class, () -> service.delete(1L));
        assertTrue(ex.getMessage().contains("4 条内容引用"), ex.getMessage());
        verify(dao, never()).removeById(any());
    }

    @Test
    void delete_allowed_when_leaf_and_unreferenced() {
        when(currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN)).thenReturn(admin());
        when(dao.getById(1L)).thenReturn(new CategoryDO());
        when(dao.count(any(Wrapper.class))).thenReturn(0L);
        when(requestCategoryDao.count(any(Wrapper.class))).thenReturn(0L);
        when(opportunityCategoryDao.count(any(Wrapper.class))).thenReturn(0L);

        service.delete(1L);
        verify(dao).removeById(1L);
    }

    @Test
    void update_rejects_self_as_parent() {
        when(currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN)).thenReturn(admin());
        CategoryDO existing = new CategoryDO();
        existing.setId(5L);
        when(dao.getById(5L)).thenReturn(existing);

        com.quectel.web.cloud.salesleadhubserver.dto.CategoryUpdateDTO dto =
                new com.quectel.web.cloud.salesleadhubserver.dto.CategoryUpdateDTO();
        dto.setId(5L);
        dto.setName("x");
        dto.setIsActive(true);
        dto.setParentId("5");   // 自己当自己的爹

        assertThrows(BaseException.class, () -> service.update(dto));
        verify(dao, never()).updateById(any(CategoryDO.class));
    }
}
