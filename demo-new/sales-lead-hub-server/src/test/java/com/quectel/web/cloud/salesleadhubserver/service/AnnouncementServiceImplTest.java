package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.AnnouncementConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.AnnouncementDao;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.AnnouncementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公告 service 行为测试：运营端鉴权边界（admin fail-closed）、create 默认草稿、
 * 状态机流转（含非法跃迁与 published_at 语义）、乐观锁冲突、前台可见性（详情 published-only）。
 *
 * <p>dao 全 mock，离线可跑。前台/运营 page 的 SQL 级"只出 published / 含草稿"由
 * {@code AnnounceIntegrationTest} 真库覆盖（MyBatis-Plus 的 LambdaQueryChainWrapper
 * 在纯单测里 mock 成本高且价值低）。</p>
 */
class AnnouncementServiceImplTest {

    @Mock AnnouncementDao dao;
    @Mock CurrentUserResolver currentUser;

    private AnnouncementServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new AnnouncementServiceImpl(dao, currentUser, new AnnouncementConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO admin(long id) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName("运营管理员");
        u.setRole(CurrentUserResolver.ROLE_ADMIN);
        u.setStatus("active");
        return u;
    }

    private AnnounceCreateDTO createDto() {
        AnnounceCreateDTO dto = new AnnounceCreateDTO();
        dto.setTitle("标题");
        dto.setType("notice");
        dto.setPriority("normal");
        dto.setContent("<p>正文</p>");
        dto.setIsPinned(false);
        dto.setBannerEnabled(false);
        return dto;
    }

    private AnnounceUpdateDTO updateDto() {
        AnnounceUpdateDTO dto = new AnnounceUpdateDTO();
        dto.setId(1L);
        dto.setVersion(3);
        dto.setTitle("改后标题");
        dto.setType("notice");
        dto.setPriority("normal");
        dto.setContent("<p>改后</p>");
        dto.setIsPinned(false);
        dto.setBannerEnabled(false);
        return dto;
    }

    // ---------- 运营端鉴权（非 admin 被拒）----------

    @Test
    void create_by_non_admin_is_denied_and_never_saves() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        assertThrows(BaseException.class, () -> service.create(createDto()));
        verify(dao, never()).save(any());
    }

    @Test
    void delete_by_non_admin_is_denied_and_never_removes() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        assertThrows(BaseException.class, () -> service.delete(1L));
        verify(dao, never()).removeById(anyLong());
    }

    @Test
    void stats_by_non_admin_is_denied() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        assertThrows(BaseException.class, () -> service.stats());
    }

    // ---------- create：payload 无 status，一律落草稿 ----------

    @Test
    void create_defaults_to_draft_and_fills_publisher_and_zero_views() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));

        Long ignored = service.create(createDto());

        ArgumentCaptor<AnnouncementDO> cap = ArgumentCaptor.forClass(AnnouncementDO.class);
        verify(dao).save(cap.capture());
        AnnouncementDO saved = cap.getValue();
        // 前端不发 status，后端强制草稿
        assertEquals("draft", saved.getStatus());
        assertNull(saved.getPublishedAt(), "草稿不应有发布时间");
        assertEquals(Integer.valueOf(0), saved.getViewCount());
        // 发布人快照取自本地档案（防伪造）
        assertEquals(Long.valueOf(9001L), saved.getPublisherId());
        assertEquals("运营管理员", saved.getPublisherName());
    }

    // ---------- update：乐观锁 ----------

    @Test
    void update_stale_version_raises_conflict() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));
        AnnouncementDO existing = new AnnouncementDO();
        existing.setId(1L);
        existing.setStatus("published");
        when(dao.getById(1L)).thenReturn(existing);
        when(dao.updateById(any())).thenReturn(false);   // WHERE version=? 没命中

        assertThrows(BaseException.class, () -> service.update(updateDto()));
    }

    @Test
    void update_missing_row_is_not_found() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));
        when(dao.getById(1L)).thenReturn(null);
        assertThrows(BaseException.class, () -> service.update(updateDto()));
        verify(dao, never()).updateById(any());
    }

    // ---------- changeStatus：状态机 ----------

    @Test
    void changeStatus_archive_draft_is_illegal_transition() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));
        AnnouncementDO d = new AnnouncementDO();
        d.setId(1L);
        d.setStatus("draft");
        when(dao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.changeStatus(1L, "archived"));
        verify(dao, never()).updateById(any());
    }

    @Test
    void changeStatus_publish_draft_sets_published_at() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));
        AnnouncementDO d = new AnnouncementDO();
        d.setId(1L);
        d.setStatus("draft");
        when(dao.getById(1L)).thenReturn(d);
        when(dao.updateById(any())).thenReturn(true);

        service.changeStatus(1L, "published");

        assertEquals("published", d.getStatus());
        assertNotNull(d.getPublishedAt(), "首次发布必须补 published_at");
    }

    @Test
    void changeStatus_republish_archived_keeps_original_published_at() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));
        LocalDateTime first = LocalDateTime.of(2026, 7, 1, 10, 0, 0);
        AnnouncementDO d = new AnnouncementDO();
        d.setId(1L);
        d.setStatus("archived");
        d.setPublishedAt(first);
        when(dao.getById(1L)).thenReturn(d);
        when(dao.updateById(any())).thenReturn(true);

        service.changeStatus(1L, "published");

        assertEquals("published", d.getStatus());
        assertEquals(first, d.getPublishedAt(), "恢复上架不得改动初次发布时间");
    }

    @Test
    void changeStatus_archive_already_archived_is_illegal() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));
        AnnouncementDO d = new AnnouncementDO();
        d.setId(1L);
        d.setStatus("archived");
        when(dao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.changeStatus(1L, "archived"));
    }

    @Test
    void changeStatus_stale_version_raises_conflict() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin(9001L));
        AnnouncementDO d = new AnnouncementDO();
        d.setId(1L);
        d.setStatus("published");
        when(dao.getById(1L)).thenReturn(d);
        when(dao.updateById(any())).thenReturn(false);

        assertThrows(BaseException.class, () -> service.changeStatus(1L, "archived"));
    }

    // ---------- 前台详情：published-only 可见性 ----------

    @Test
    void frontDetail_of_draft_is_not_found_and_never_increments() {
        AnnouncementDO d = new AnnouncementDO();
        d.setId(1L);
        d.setStatus("draft");
        when(dao.getById(1L)).thenReturn(d);

        BaseException ex = assertThrows(BaseException.class, () -> service.frontDetail(1L));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("未发布"), ex.getMessage());
        // 未发布不应触发浏览数自增
        verify(dao, never()).lambdaUpdate();
    }

    @Test
    void frontDetail_of_archived_is_not_found() {
        AnnouncementDO d = new AnnouncementDO();
        d.setId(1L);
        d.setStatus("archived");
        when(dao.getById(1L)).thenReturn(d);
        assertThrows(BaseException.class, () -> service.frontDetail(1L));
    }

    @Test
    void frontDetail_missing_row_is_not_found() {
        when(dao.getById(1L)).thenReturn(null);
        assertThrows(BaseException.class, () -> service.frontDetail(1L));
    }
}
