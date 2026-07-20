package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.AuditConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditChangePinDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditPageDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.AuditServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 运营审核 service 行为测试：鉴权边界（非 admin 全端点被拒）、非法目标状态被拒、
 * changePin 写 is_pinned、按 id/status 分流到正确的 Dao。dao 全 mock，离线可跑。
 */
class AuditServiceImplTest {

    @Mock OpportunityDao opportunityDao;
    @Mock RequirementDao requirementDao;
    @Mock CurrentUserResolver currentUser;

    private AuditServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new AuditServiceImpl(opportunityDao, requirementDao, currentUser, new AuditConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO admin() {
        SysUserDO u = new SysUserDO();
        u.setId(9001L);
        u.setName("运营");
        u.setRole(CurrentUserResolver.ROLE_ADMIN);
        u.setStatus("active");
        return u;
    }

    private void denyRole() {
        when(currentUser.requireAnyRole(any()))
                .thenThrow(new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
    }

    // ---------- 鉴权：非 admin 全端点被拒 ----------

    @Test
    void page_denied_for_non_admin() {
        denyRole();
        assertThrows(BaseException.class, () -> service.page(new AuditPageDTO()));
    }

    @Test
    void changeStatus_denied_for_non_admin_never_touches_dao() {
        denyRole();
        assertThrows(BaseException.class, () -> service.changeStatus(1L, "archived"));
        verify(opportunityDao, never()).updateById(any());
        verify(requirementDao, never()).updateById(any());
    }

    @Test
    void changePin_denied_for_non_admin_never_touches_dao() {
        denyRole();
        AuditChangePinDTO dto = new AuditChangePinDTO();
        dto.setId(1L);
        dto.setIsPinned(true);
        assertThrows(BaseException.class, () -> service.changePin(dto));
        verify(opportunityDao, never()).updateById(any());
        verify(requirementDao, never()).updateById(any());
    }

    @Test
    void delete_denied_for_non_admin_never_touches_dao() {
        denyRole();
        assertThrows(BaseException.class, () -> service.delete(1L));
        verify(opportunityDao, never()).removeById(any());
        verify(requirementDao, never()).removeById(any());
    }

    // ---------- changeStatus：非法目标状态被拒 ----------

    @Test
    void changeStatus_illegal_target_status_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        // Adopted 既不是商机的 published/archived，也不是需求下架目标 Closed
        assertThrows(BaseException.class, () -> service.changeStatus(1L, "Adopted"));
        verify(opportunityDao, never()).updateById(any());
        verify(requirementDao, never()).updateById(any());
    }

    // ---------- changeStatus：分流 + 语义 ----------

    @Test
    void changeStatus_archived_routes_to_opportunity_and_stamps_archiver() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        d.setStatus("published");
        when(opportunityDao.getById(1L)).thenReturn(d);
        when(opportunityDao.updateById(any())).thenReturn(true);

        service.changeStatus(1L, "archived");

        ArgumentCaptor<OpportunityDO> cap = ArgumentCaptor.forClass(OpportunityDO.class);
        verify(opportunityDao).updateById(cap.capture());
        assertEquals("archived", cap.getValue().getStatus());
        assertEquals(Long.valueOf(9001L), cap.getValue().getArchivedBy());
        // 走商机分支，不碰需求 Dao
        verify(requirementDao, never()).getById(any());
        verify(requirementDao, never()).updateById(any());
    }

    @Test
    void changeStatus_closed_routes_to_request_dao() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        OpportunityRequestDO r = new OpportunityRequestDO();
        r.setId(2L);
        r.setStatus("Collecting");
        when(requirementDao.getById(2L)).thenReturn(r);
        when(requirementDao.updateById(any())).thenReturn(true);

        service.changeStatus(2L, "Closed");

        ArgumentCaptor<OpportunityRequestDO> cap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
        verify(requirementDao).updateById(cap.capture());
        assertEquals("Closed", cap.getValue().getStatus());
        // 走需求分支，不碰商机 Dao
        verify(opportunityDao, never()).getById(any());
        verify(opportunityDao, never()).updateById(any());
    }

    @Test
    void changeStatus_archive_non_published_is_illegal_transition() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        d.setStatus("archived");   // 已下架还想下架
        when(opportunityDao.getById(1L)).thenReturn(d);

        assertThrows(BaseException.class, () -> service.changeStatus(1L, "archived"));
        verify(opportunityDao, never()).updateById(any());
    }

    // ---------- changePin：写 is_pinned + 分流 ----------

    @Test
    void changePin_on_opportunity_writes_is_pinned() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        d.setIsPinned(false);
        when(opportunityDao.getById(1L)).thenReturn(d);
        when(opportunityDao.updateById(any())).thenReturn(true);

        AuditChangePinDTO dto = new AuditChangePinDTO();
        dto.setId(1L);
        dto.setIsPinned(true);
        service.changePin(dto);

        ArgumentCaptor<OpportunityDO> cap = ArgumentCaptor.forClass(OpportunityDO.class);
        verify(opportunityDao).updateById(cap.capture());
        assertTrue(cap.getValue().getIsPinned());
        verify(requirementDao, never()).updateById(any());
    }

    @Test
    void changePin_falls_through_to_request_when_not_opportunity() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        when(opportunityDao.getById(3L)).thenReturn(null);   // 不在商机表
        OpportunityRequestDO r = new OpportunityRequestDO();
        r.setId(3L);
        r.setIsPinned(false);
        when(requirementDao.getById(3L)).thenReturn(r);
        when(requirementDao.updateById(any())).thenReturn(true);

        AuditChangePinDTO dto = new AuditChangePinDTO();
        dto.setId(3L);
        dto.setIsPinned(true);
        dto.setSortNo(7);
        service.changePin(dto);

        ArgumentCaptor<OpportunityRequestDO> cap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
        verify(requirementDao).updateById(cap.capture());
        assertTrue(cap.getValue().getIsPinned());
        assertEquals(Integer.valueOf(7), cap.getValue().getSortNo());
        verify(opportunityDao, never()).updateById(any());
    }

    // ---------- delete：分流 + 逻辑删 ----------

    @Test
    void delete_routes_to_opportunity_logical_delete() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        OpportunityDO d = new OpportunityDO();
        d.setId(1L);
        when(opportunityDao.getById(1L)).thenReturn(d);

        service.delete(1L);

        verify(opportunityDao).removeById(1L);
        verify(requirementDao, never()).removeById(any());
    }

    @Test
    void delete_missing_id_raises_not_found() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        when(opportunityDao.getById(99L)).thenReturn(null);
        when(requirementDao.getById(99L)).thenReturn(null);

        assertThrows(BaseException.class, () -> service.delete(99L));
    }
}
