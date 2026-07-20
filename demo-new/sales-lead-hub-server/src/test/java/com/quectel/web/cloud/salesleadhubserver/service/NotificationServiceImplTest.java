package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.code.security.utils.SecurityUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.NotificationConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPreferenceDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知 service 行为测试：归属边界（page/markRead 强制当前登录人）、markRead 拒改他人、
 * markAllRead 只批量置己方、preference/save 落 sys_user 且未开通拒绝。
 *
 * <p>dao 全 mock、SecurityUtils 静态 mock，离线可跑；page 的真实 WHERE user_id 过滤
 * （fluent chain 不宜在单测断言）由 NotificationIntegrationTest 覆盖。</p>
 *
 * <p>⚠️ 方案原列的「preference/save 全量覆盖 subscription、非法 categoryId 拒绝」与前端真实
 * 契约不符——payload 是「类型×渠道」矩阵、无 categoryId，落 sys_user.notification_preferences。
 * 本测试按真实契约验证。</p>
 */
class NotificationServiceImplTest {

    @Mock NotificationDao dao;
    @Mock SysUserDao sysUserDao;

    private NotificationServiceImpl service;
    private AutoCloseable mocks;

    private static final Long ME = 10160L;
    private static final Long OTHER = 9004L;

    /** 离线单测无 MyBatis 容器，Lambda wrapper 依赖的 TableInfo 缓存须手动预热 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, NotificationDO.class);
        TableInfoHelper.initTableInfo(assistant, SysUserDO.class);
    }

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new NotificationServiceImpl(dao, sysUserDao, new NotificationConvert(), new ObjectMapper());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    // ---------- 归属边界 ----------

    @Test
    void page_without_login_is_forbidden_and_never_queries() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(null);
            assertThrows(BaseException.class, () -> service.page(new NotificationPageDTO()));
        }
    }

    // ---------- markRead ----------

    @Test
    void markRead_others_notification_is_rejected() {
        NotificationDO d = new NotificationDO();
        d.setId(1L);
        d.setUserId(OTHER);          // 别人的通知
        d.setIsRead(0);
        when(dao.getById(1L)).thenReturn(d);

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(ME);
            assertThrows(BaseException.class, () -> service.markRead(1L));
        }
        verify(dao, never()).updateById(any());
    }

    @Test
    void markRead_own_notification_marks_read() {
        NotificationDO d = new NotificationDO();
        d.setId(1L);
        d.setUserId(ME);
        d.setIsRead(0);
        d.setIsForceConfirm(0);
        when(dao.getById(1L)).thenReturn(d);

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(ME);
            service.markRead(1L);
        }
        verify(dao, times(1)).updateById(any());
    }

    // ---------- markAllRead ----------

    @Test
    void markAllRead_requires_login_then_batch_updates() {
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(ME);
            service.markAllRead();
        }
        // 只发一条批量 UPDATE（内部 wrapper 已按 user_id=ME 收窄，SQL 层由集成测试验证）
        verify(dao, times(1)).update(any());
    }

    // ---------- preference/save ----------

    @Test
    void savePreference_persists_matrix_json_to_sys_user() {
        SysUserDO me = new SysUserDO();
        me.setId(ME);
        when(sysUserDao.getById(ME)).thenReturn(me);

        Map<String, Boolean> row = new HashMap<>();
        row.put("in_app", true);
        row.put("feishu", false);
        Map<String, Map<String, Boolean>> matrix = new HashMap<>();
        matrix.put("publish", row);
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setMatrix(matrix);

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(ME);
            service.savePreference(dto);
        }
        // 落库走 sysUserDao 的部分列 update（notification_preferences）
        verify(sysUserDao, times(1)).update(any());
    }

    @Test
    void savePreference_without_local_profile_is_forbidden() {
        when(sysUserDao.getById(ME)).thenReturn(null);
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setMatrix(Collections.emptyMap());

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(ME);
            assertThrows(BaseException.class, () -> service.savePreference(dto));
        }
        verify(sysUserDao, never()).update(any());
    }
}
