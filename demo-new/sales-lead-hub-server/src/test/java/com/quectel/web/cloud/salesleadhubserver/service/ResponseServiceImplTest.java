package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementAdoptDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCloseDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.ResponseCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SolutionResponseDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.ResponseServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 需求-方案匹配闭环单测：提交方案停表+计数+通知；采纳双写同事务+非发布人拒+重复采纳拒；关闭状态机。
 */
class ResponseServiceImplTest {

    @Mock RequirementDao requirementDao;
    @Mock SolutionResponseDao solutionResponseDao;
    @Mock NotificationDao notificationDao;
    @Mock CurrentUserResolver currentUser;
    @InjectMocks ResponseServiceImpl service;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(Long id, String name, String role) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        u.setStatus("active");
        u.setDepartmentId(1001L);
        u.setDepartmentName("上海销售组");
        return u;
    }

    private void authorizedAs(SysUserDO me) {
        when(currentUser.requireAnyRole(anyString(), anyString(), anyString())).thenReturn(me);
    }

    private OpportunityRequestDO req(Long id, Long publisherId, String status) {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(id);
        d.setPublisherId(publisherId);
        d.setStatus(status);
        d.setTitle("5G 选型需求");
        d.setVersion(1);
        return d;
    }

    private ResponseCreateDTO createDto(Long requestId) {
        ResponseCreateDTO dto = new ResponseCreateDTO();
        dto.setRequestId(requestId);
        dto.setContent("<p>建议选 RG500Q</p>");
        dto.setFeishuSync(Boolean.TRUE);
        return dto;
    }

    // —— 提交方案 ——

    @Test
    void create_saves_response_increments_count_and_notifies_publisher() {
        authorizedAs(user(7001L, "响应人", CurrentUserResolver.ROLE_SALES));
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Pending"));
        when(solutionResponseDao.save(any())).thenAnswer(inv -> {
            ((SolutionResponseDO) inv.getArgument(0)).setId(555L);
            return true;
        });

        Long id = service.create(createDto(100L));

        assertEquals(Long.valueOf(555L), id);
        // 停表 + 计数：交给 Dao 原子推进（含 Pending→Collecting）
        verify(requirementDao).increaseResponseCount(100L);
        // 响应人快照落库
        ArgumentCaptor<SolutionResponseDO> respCap = ArgumentCaptor.forClass(SolutionResponseDO.class);
        verify(solutionResponseDao).save(respCap.capture());
        assertEquals(Long.valueOf(7001L), respCap.getValue().getResponderId(), "响应人快照取自本地档案");
        assertEquals(Integer.valueOf(1), respCap.getValue().getFeishuSync(), "布尔飞书开关落库为 1");
        // 通知需求发布人
        ArgumentCaptor<NotificationDO> notiCap = ArgumentCaptor.forClass(NotificationDO.class);
        verify(notificationDao).save(notiCap.capture());
        assertEquals(Long.valueOf(9002L), notiCap.getValue().getUserId(), "通知接收人=发布人");
        assertEquals("response", notiCap.getValue().getType());
    }

    @Test
    void create_rejects_when_requirement_closed() {
        authorizedAs(user(7001L, "响应人", CurrentUserResolver.ROLE_SALES));
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Closed"));

        assertThrows(BaseException.class, () -> service.create(createDto(100L)));
        verify(solutionResponseDao, never()).save(any());
    }

    // —— 采纳 ——

    @Test
    void adopt_double_writes_requirement_and_response_and_notifies() {
        authorizedAs(user(9002L, "发布人", CurrentUserResolver.ROLE_SALES));  // 发布人本人
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Collecting"));
        SolutionResponseDO resp = new SolutionResponseDO();
        resp.setId(555L);
        resp.setRequestId(100L);
        resp.setResponderId(7001L);
        when(solutionResponseDao.getById(555L)).thenReturn(resp);
        when(requirementDao.updateById(any())).thenReturn(true);

        RequirementAdoptDTO dto = new RequirementAdoptDTO();
        dto.setId(100L);
        dto.setResponseId(555L);
        service.adopt(dto);

        ArgumentCaptor<OpportunityRequestDO> reqCap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
        verify(requirementDao).updateById(reqCap.capture());
        assertEquals(Long.valueOf(555L), reqCap.getValue().getAdoptedResponseId(), "需求记采纳方案(单一真相源)");
        assertEquals("Adopted", reqCap.getValue().getStatus());
        ArgumentCaptor<SolutionResponseDO> respCap = ArgumentCaptor.forClass(SolutionResponseDO.class);
        verify(solutionResponseDao).updateById(respCap.capture());
        assertEquals(Integer.valueOf(1), respCap.getValue().getIsAdopted(), "方案冗余标记同事务置 1");
        // 通知响应人
        ArgumentCaptor<NotificationDO> notiCap = ArgumentCaptor.forClass(NotificationDO.class);
        verify(notificationDao).save(notiCap.capture());
        assertEquals(Long.valueOf(7001L), notiCap.getValue().getUserId());
        assertEquals("adopt", notiCap.getValue().getType());
    }

    @Test
    void adopt_rejects_non_owner() {
        authorizedAs(user(8888L, "路人", CurrentUserResolver.ROLE_SALES));   // 非发布人、非 admin
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Collecting"));

        RequirementAdoptDTO dto = new RequirementAdoptDTO();
        dto.setId(100L);
        dto.setResponseId(555L);
        assertThrows(BaseException.class, () -> service.adopt(dto));
        verify(requirementDao, never()).updateById(any());
    }

    @Test
    void adopt_rejects_when_already_adopted() {
        authorizedAs(user(9002L, "发布人", CurrentUserResolver.ROLE_SALES));
        OpportunityRequestDO already = req(100L, 9002L, "Adopted");
        already.setAdoptedResponseId(999L);
        when(requirementDao.getById(100L)).thenReturn(already);

        RequirementAdoptDTO dto = new RequirementAdoptDTO();
        dto.setId(100L);
        dto.setResponseId(555L);
        assertThrows(BaseException.class, () -> service.adopt(dto), "已采纳需求不能二次采纳");
        verify(requirementDao, never()).updateById(any());
    }

    @Test
    void adopt_rejects_response_not_belonging_to_requirement() {
        authorizedAs(user(9002L, "发布人", CurrentUserResolver.ROLE_SALES));
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Collecting"));
        SolutionResponseDO otherReqResp = new SolutionResponseDO();
        otherReqResp.setId(555L);
        otherReqResp.setRequestId(200L);   // 属于别的需求
        when(solutionResponseDao.getById(555L)).thenReturn(otherReqResp);

        RequirementAdoptDTO dto = new RequirementAdoptDTO();
        dto.setId(100L);
        dto.setResponseId(555L);
        assertThrows(BaseException.class, () -> service.adopt(dto));
        verify(requirementDao, never()).updateById(any());
    }

    // —— 关闭状态机 ——

    @Test
    void close_pending_to_closed() {
        authorizedAs(user(9002L, "发布人", CurrentUserResolver.ROLE_SALES));
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Pending"));
        when(requirementDao.updateById(any())).thenReturn(true);

        RequirementCloseDTO dto = new RequirementCloseDTO();
        dto.setId(100L);
        service.close(dto);

        ArgumentCaptor<OpportunityRequestDO> cap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
        verify(requirementDao).updateById(cap.capture());
        assertEquals("Closed", cap.getValue().getStatus());
    }

    @Test
    void close_rejects_when_already_adopted() {
        authorizedAs(user(9002L, "发布人", CurrentUserResolver.ROLE_SALES));
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Adopted"));

        RequirementCloseDTO dto = new RequirementCloseDTO();
        dto.setId(100L);
        assertThrows(BaseException.class, () -> service.close(dto), "非 Pending/Collecting 不可关闭");
        verify(requirementDao, never()).updateById(any());
    }

    @Test
    void close_rejects_non_owner() {
        authorizedAs(user(8888L, "路人", CurrentUserResolver.ROLE_SALES));
        when(requirementDao.getById(100L)).thenReturn(req(100L, 9002L, "Pending"));

        RequirementCloseDTO dto = new RequirementCloseDTO();
        dto.setId(100L);
        assertThrows(BaseException.class, () -> service.close(dto));
        verify(requirementDao, never()).updateById(any());
    }
}
