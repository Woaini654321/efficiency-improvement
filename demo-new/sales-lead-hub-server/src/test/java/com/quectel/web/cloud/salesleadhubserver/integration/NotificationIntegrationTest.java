package com.quectel.web.cloud.salesleadhubserver.integration;

import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPreferenceDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.NotificationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

/**
 * 通知模块真库集成测试（需 MySQL 可达；类名以 Test 结尾 + @Tag 隔离，理由见 OpportunityIntegrationTest）。
 *
 * <p>覆盖 markRead 归属校验（拒改他人）与 preference/save 覆盖语义（后写整体替换先写）。
 * 用 {@code MockedStatic<SecurityUtils>} 在测试线程内注入当前登录人；测试数据自建自删。</p>
 */
@Tag("integration")
@SpringBootTest
class NotificationIntegrationTest {

    /** data.sql 的销售账号张伟（sys_user.id=9002），preference 落在其档案上。 */
    private static final Long ME = 9002L;
    /** 一个明显非本人的接收人 id（无需存在 sys_user 行，仅用于通知归属判定）。 */
    private static final Long OTHER = 987654321098765432L;

    @Autowired
    NotificationDao notificationDao;

    @Autowired
    SysUserDao sysUserDao;

    @Autowired
    NotificationService service;

    @Test
    void markRead_enforces_ownership() {
        NotificationDO mine = insertNotification(ME);
        NotificationDO others = insertNotification(OTHER);
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(ME);

            // 自己的：标记成功，库里 is_read=1
            service.markRead(mine.getId());
            assertEquals(Integer.valueOf(1), notificationDao.getById(mine.getId()).getIsRead(),
                    "本人通知应被标记已读");

            // 他人的：拒绝，且库里 is_read 保持 0
            assertThrows(BaseException.class, () -> service.markRead(others.getId()),
                    "不得标记他人通知");
            assertEquals(Integer.valueOf(0), notificationDao.getById(others.getId()).getIsRead(),
                    "他人通知不应被改动");
        } finally {
            notificationDao.removeById(mine.getId());
            notificationDao.removeById(others.getId());
        }
    }

    @Test
    void savePreference_is_full_overwrite() {
        SysUserDO before = sysUserDao.getById(ME);
        assertNotNull(before, "前置数据缺失：data.sql 应有 sys_user id=9002");
        String original = before.getNotificationPreferences();
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(ME);

            service.savePreference(matrix("publish", "feishu"));
            String first = sysUserDao.getById(ME).getNotificationPreferences();
            assertTrue(first.contains("publish"), "首存应含 publish：" + first);

            // 覆盖语义：后写整体替换先写，不残留旧键
            service.savePreference(matrix("system", "email"));
            String second = sysUserDao.getById(ME).getNotificationPreferences();
            assertTrue(second.contains("system"), "覆盖后应含 system：" + second);
            assertTrue(!second.contains("publish"), "覆盖后不应残留旧键 publish：" + second);
        } finally {
            // 还原原值，避免污染共享库
            sysUserDao.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SysUserDO>()
                    .eq(SysUserDO::getId, ME)
                    .set(SysUserDO::getNotificationPreferences, original));
        }
    }

    private NotificationDO insertNotification(Long userId) {
        NotificationDO n = new NotificationDO();
        n.setUserId(userId);
        n.setType("system");
        n.setChannel("in_app");
        n.setTitle("集成测试通知");
        n.setIsRead(0);
        n.setIsForceConfirm(0);
        notificationDao.save(n);
        assertNotNull(n.getId(), "雪花 id 应回填");
        return n;
    }

    private NotificationPreferenceDTO matrix(String type, String channel) {
        Map<String, Boolean> row = new HashMap<>();
        row.put(channel, true);
        Map<String, Map<String, Boolean>> m = new HashMap<>();
        m.put(type, row);
        NotificationPreferenceDTO dto = new NotificationPreferenceDTO();
        dto.setMatrix(m == null ? Collections.emptyMap() : m);
        return dto;
    }
}
