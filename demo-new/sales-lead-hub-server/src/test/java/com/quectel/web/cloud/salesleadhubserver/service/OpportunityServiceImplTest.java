package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.OpportunityConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.OpportunityServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 商机 service 行为测试：鉴权边界、按状态条件校验、状态机流转、乐观锁冲突。
 * dao 全 mock，离线可跑（真库行为由 OpportunityIntegrationTest 覆盖）。
 */
class OpportunityServiceImplTest {

    @Mock OpportunityDao dao;
    @Mock CategoryDao categoryDao;
    @Mock OpportunityCategoryDao opportunityCategoryDao;
    @Mock SysUserDao sysUserDao;
    @Mock CurrentUserResolver currentUser;

    private OpportunityServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new OpportunityServiceImpl(dao, categoryDao, opportunityCategoryDao,
                sysUserDao, currentUser, new OpportunityConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(long id, String role) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName("用户" + id);
        u.setRole(role);
        u.setStatus("active");
        u.setDepartmentId(1001L);
        u.setDepartmentName("上海销售组");
        return u;
    }

    private OpportunityCreateDTO draftDto() {
        OpportunityCreateDTO dto = new OpportunityCreateDTO();
        dto.setTitle("标题");
        dto.setType("solution");
        dto.setStatus("draft");
        return dto;
    }

    // ---------- create ----------

    @Test
    void create_denied_propagates_and_never_saves() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(com.quectel.code.web.exception.ErrorCode.FORBIDDEN, "拒绝"));
        assertThrows(BaseException.class, () -> service.create(draftDto()));
        verify(dao, never()).save(any());
    }

    @Test
    void create_draft_needs_only_title_and_fills_publisher_from_me() {
        SysUserDO me = user(10160L, CurrentUserResolver.ROLE_SALES);
        when(currentUser.requireAnyRole(any())).thenReturn(me);

        service.create(draftDto());

        org.mockito.ArgumentCaptor<OpportunityDO> cap =
                org.mockito.ArgumentCaptor.forClass(OpportunityDO.class);
        verify(dao).save(cap.capture());
        OpportunityDO saved = cap.getValue();
        assertEquals(Long.valueOf(10160L), saved.getPublisherId());
        assertEquals("上海销售组", saved.getPublisherDeptName());
        assertEquals("draft", saved.getStatus());
        assertEquals(Integer.valueOf(0), saved.getViewCount());
    }

    @Test
    void create_published_without_categories_is_rejected() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(10160L, CurrentUserResolver.ROLE_SALES));
        OpportunityCreateDTO dto = draftDto();
        dto.setStatus("published");
        dto.setContent("<p>正文</p>");
        dto.setCategoryIds(Collections.emptyList());

        assertThrows(BaseException.class, () -> service.create(dto), "发布须 1~5 个分类");
        verify(dao, never()).save(any());
    }

    @Test
    void create_archived_via_create_is_rejected() {
        // archived 只能走 changeStatus，否则「下架人」语义被绕过
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(10160L, CurrentUserResolver.ROLE_SALES));
        OpportunityCreateDTO dto = draftDto();
        dto.setStatus("archived");
        assertThrows(BaseException.class, () -> service.create(dto));
    }

    @Test
    void proxy_publish_by_sales_is_forbidden() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(10160L, CurrentUserResolver.ROLE_SALES));
        OpportunityCreateDTO dto = draftDto();
        dto.setPublisherId("9002");   // 不是本人

        assertThrows(BaseException.class, () -> service.create(dto), "销售不可代发布");
        verify(dao, never()).save(any());
    }

    @Test
    void proxy_publish_by_pm_snapshots_target_profile() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(9003L, CurrentUserResolver.ROLE_PRODUCT_MANAGER));
        when(sysUserDao.getById(9002L)).thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        OpportunityCreateDTO dto = draftDto();
        dto.setPublisherId("9002");

        service.create(dto);

        org.mockito.ArgumentCaptor<OpportunityDO> cap =
                org.mockito.ArgumentCaptor.forClass(OpportunityDO.class);
        verify(dao).save(cap.capture());
        // 快照必须是代发目标的档案，不是操作人的
        assertEquals(Long.valueOf(9002L), cap.getValue().getPublisherId());
    }

    // ---------- update ----------

    @Test
    void update_by_stranger_is_rejected() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(9004L, CurrentUserResolver.ROLE_SALES));
        OpportunityDO existing = new OpportunityDO();
        existing.setId(1L);
        existing.setPublisherId(10160L);   // 别人的
        when(dao.getById(1L)).thenReturn(existing);

        OpportunityUpdateDTO dto = updateDto();
        assertThrows(BaseException.class, () -> service.update(dto));
        verify(dao, never()).updateById(any());
    }

    @Test
    void update_stale_version_raises_conflict() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(10160L, CurrentUserResolver.ROLE_SALES));
        OpportunityDO existing = new OpportunityDO();
        existing.setId(1L);
        existing.setPublisherId(10160L);
        when(dao.getById(1L)).thenReturn(existing);
        when(dao.updateById(any())).thenReturn(false);   // WHERE version=? 没命中

        assertThrows(BaseException.class, () -> service.update(updateDto()));
    }

    private OpportunityUpdateDTO updateDto() {
        OpportunityUpdateDTO dto = new OpportunityUpdateDTO();
        dto.setId(1L);
        dto.setVersion(3);
        dto.setTitle("标题");
        dto.setType("solution");
        dto.setStatus("draft");
        return dto;
    }

    // ---------- changeStatus ----------

    @Test
    void archive_draft_is_illegal_transition() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(10160L, CurrentUserResolver.ROLE_SALES));
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        d.setStatus("draft");
        d.setPublisherId(10160L);
        when(dao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.changeStatus(1L, "archived"));
    }

    @Test
    void restore_by_non_archiver_is_rejected() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(9004L, CurrentUserResolver.ROLE_SALES));
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        d.setStatus("archived");
        d.setPublisherId(9004L);
        d.setArchivedBy(9001L);   // 是管理员下架的，发布人自己也不能恢复
        when(dao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.changeStatus(1L, "published"));
    }

    @Test
    void restore_by_archiver_clears_archived_by() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(10160L, CurrentUserResolver.ROLE_SALES));
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        d.setStatus("archived");
        d.setPublisherId(10160L);
        d.setArchivedBy(10160L);
        when(dao.getById(1L)).thenReturn(d);
        when(dao.updateById(any())).thenReturn(true);

        service.changeStatus(1L, "published");

        assertEquals("published", d.getStatus());
        assertNull(d.getArchivedBy());
    }

    // ---------- detail ----------

    @Test
    void draft_detail_hidden_from_others() {
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        d.setStatus("draft");
        d.setPublisherId(10160L);
        when(dao.getById(1L)).thenReturn(d);

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(9004L);   // 别人
            BaseException ex = assertThrows(BaseException.class, () -> service.detail(1L));
            assertTrue(ex.getMessage().contains("草稿"), ex.getMessage());
        }
    }
}
