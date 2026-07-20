package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.SlaConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.SlaPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.SlaUrgeDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.SlaServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SLA service 行为测试：鉴权边界（仅 admin）+ 催办按类别解析写通知。
 * 派生逻辑的矩阵覆盖在 {@link SlaCalculatorTest}；此处 dao 全 mock、离线可跑。
 */
class SlaServiceImplTest {

    @Mock RequirementDao requirementDao;
    @Mock ProductLineMemberDao productLineMemberDao;
    @Mock ProductLineDao productLineDao;
    @Mock RequestProductLineDao requestProductLineDao;
    @Mock SysDepartmentDao sysDepartmentDao;
    @Mock SysUserDao sysUserDao;
    @Mock NotificationDao notificationDao;
    @Mock CurrentUserResolver currentUser;

    private SlaServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new SlaServiceImpl(requirementDao, productLineMemberDao, productLineDao,
                requestProductLineDao, sysDepartmentDao, sysUserDao, notificationDao,
                currentUser, new SlaCalculator(), new SlaConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO admin() {
        SysUserDO u = new SysUserDO();
        u.setId(9001L);
        u.setName("运营管理员");
        u.setRole(CurrentUserResolver.ROLE_ADMIN);
        u.setStatus("active");
        return u;
    }

    // ---------- 鉴权：仅 admin ----------

    @Test
    void all_endpoints_denied_for_non_admin() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(ErrorCode.FORBIDDEN, "需要 admin"));

        assertThrows(BaseException.class, () -> service.page(new SlaPageDTO()));
        assertThrows(BaseException.class, () -> service.stats());
        assertThrows(BaseException.class, () -> service.meta());

        SlaUrgeDTO dto = new SlaUrgeDTO();
        dto.setId("1");
        dto.setTargets(Arrays.asList("publisher"));
        dto.setMethods(Arrays.asList("in_app"));
        assertThrows(BaseException.class, () -> service.urge(dto));
        verify(notificationDao, never()).saveBatch(anyList());
    }

    // ---------- 催办：publisher 类别 → 本地通知 ----------

    @Test
    void urge_publisher_writes_in_app_notification_for_publisher() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        OpportunityRequestDO req = new OpportunityRequestDO();
        req.setId(5L);
        req.setTitle("求定位模组选型");
        req.setPublisherId(900L);
        when(requirementDao.getById(5L)).thenReturn(req);
        when(notificationDao.saveBatch(anyList())).thenReturn(true);
        // 幂等窗查询：10 分钟内无既有 sla_remind → 放行本次催办
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<NotificationDO> chain =
                org.mockito.Mockito.mock(
                        com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper.class,
                        org.mockito.Answers.RETURNS_SELF);
        when(chain.exists()).thenReturn(false);
        when(notificationDao.lambdaQuery()).thenReturn(chain);

        SlaUrgeDTO dto = new SlaUrgeDTO();
        dto.setId("5");
        dto.setTargets(Arrays.asList("publisher"));
        dto.setMethods(Arrays.asList("in_app"));
        service.urge(dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NotificationDO>> cap = ArgumentCaptor.forClass(List.class);
        verify(notificationDao).saveBatch(cap.capture());
        List<NotificationDO> rows = cap.getValue();
        assertEquals(1, rows.size());
        assertEquals(Long.valueOf(900L), rows.get(0).getUserId());
        assertEquals("sla_remind", rows.get(0).getType());
        assertEquals("in_app", rows.get(0).getChannel());
    }

    @Test
    void urge_unknown_request_is_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        when(requirementDao.getById(404L)).thenReturn(null);

        SlaUrgeDTO dto = new SlaUrgeDTO();
        dto.setId("404");
        dto.setTargets(Arrays.asList("publisher"));
        dto.setMethods(Arrays.asList("in_app"));
        assertThrows(BaseException.class, () -> service.urge(dto));
        verify(notificationDao, never()).saveBatch(anyList());
    }
}
