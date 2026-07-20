package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.web.cloud.salesleadhubserver.convert.HomeConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.AnnouncementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionPostDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SubscriptionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ViewLogDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.HomeServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeDashboardVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * 首页 service 行为测试：统计四数聚合、待办过滤当前人、逾期判定。
 * 取数走 IService 的 count/list/page(wrapper) 方法，均可直接 mock。
 */
class HomeServiceImplTest {

    @Mock OpportunityDao opportunityDao;
    @Mock RequirementDao requirementDao;
    @Mock DiscussionPostDao discussionPostDao;
    @Mock MeetingTaskDao meetingTaskDao;
    @Mock AnnouncementDao announcementDao;
    @Mock ViewLogDao viewLogDao;
    @Mock SubscriptionDao subscriptionDao;
    @Mock OpportunityCategoryDao opportunityCategoryDao;
    @Mock RequestCategoryDao requestCategoryDao;
    @Mock CategoryDao categoryDao;
    @Mock CurrentUserResolver currentUser;

    private HomeServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new HomeServiceImpl(opportunityDao, requirementDao, discussionPostDao,
                meetingTaskDao, announcementDao, viewLogDao, subscriptionDao,
                opportunityCategoryDao, requestCategoryDao, categoryDao, currentUser, new HomeConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private ViewLogDO view(long userId) {
        ViewLogDO v = new ViewLogDO();
        v.setUserId(userId);
        v.setViewedAt(LocalDateTime.now().minusHours(1));
        return v;
    }

    private MeetingTaskDO task(long id, String desc, String status, List<Long> ids, List<String> names, LocalDateTime deadline) {
        MeetingTaskDO t = new MeetingTaskDO();
        t.setId(id);
        t.setTaskDesc(desc);
        t.setStatus(status);
        t.setAssigneeIds(ids);
        t.setAssigneeNames(names);
        t.setDeadline(deadline);
        return t;
    }

    @Test
    void stats_aggregate_four_numbers_and_quick_tasks_filter_current_person() {
        SysUserDO me = new SysUserDO();
        me.setId(100L);
        me.setName("张三");
        when(currentUser.currentOrNull()).thenReturn(me);

        when(opportunityDao.count(any())).thenReturn(10L);
        when(requirementDao.count(any())).thenReturn(3L);
        when(discussionPostDao.count(any())).thenReturn(5L);
        // 4 条浏览，去重用户 3 个（1,1,2,3）
        when(viewLogDao.list(any(Wrapper.class))).thenReturn(Arrays.asList(view(1L), view(1L), view(2L), view(3L)));
        when(categoryDao.list(any(Wrapper.class))).thenReturn(Collections.emptyList());   // hotTags 短路为空
        doReturn(new Page<>()).when(opportunityDao).page(any(), any());       // hotSolutions 空
        doReturn(new Page<>()).when(announcementDao).page(any(), any());      // announcements 空
        when(discussionPostDao.list()).thenReturn(Collections.emptyList());   // hotPosts 空

        LocalDateTime now = LocalDateTime.now();
        MeetingTaskDO mineOverdue = task(1L, "T1", "pending", Arrays.asList(100L), null, now.minusDays(1));
        MeetingTaskDO other = task(2L, "T2", "pending", Arrays.asList(200L), null, now.plusDays(2));
        MeetingTaskDO mineByName = task(3L, "T3", "processing", null, Arrays.asList("张三"), now.plusDays(1));
        when(meetingTaskDao.list(any(Wrapper.class))).thenReturn(Arrays.asList(mineOverdue, other, mineByName));

        HomeDashboardVO vo = service.dashboard();

        assertEquals(Integer.valueOf(10), vo.getStats().getSolutionTotal());
        assertEquals(Integer.valueOf(3), vo.getStats().getPendingRequests());
        assertEquals(Integer.valueOf(5), vo.getStats().getWeekDiscussions());
        assertEquals(Integer.valueOf(3), vo.getStats().getActiveUsers());

        // 只保留当前人的两条（id 命中 + 姓名命中），别人的被过滤；deadline 正序
        assertEquals(2, vo.getQuickTasks().size());
        assertEquals("T1", vo.getQuickTasks().get(0).getTitle());
        assertTrue(vo.getQuickTasks().get(0).getIsOverdue(), "过去 deadline 应判逾期");
        assertEquals("T3", vo.getQuickTasks().get(1).getTitle());
        assertFalse(vo.getQuickTasks().get(1).getIsOverdue(), "未来 deadline 不逾期");
    }

    @Test
    void anonymous_user_gets_empty_personalized_sections_but_stats_still_run() {
        when(currentUser.currentOrNull()).thenReturn(null);
        when(opportunityDao.count(any())).thenReturn(0L);
        when(requirementDao.count(any())).thenReturn(0L);
        when(discussionPostDao.count(any())).thenReturn(0L);
        when(viewLogDao.list(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(categoryDao.list(any(Wrapper.class))).thenReturn(Collections.emptyList());
        doReturn(new Page<>()).when(opportunityDao).page(any(), any());
        doReturn(new Page<>()).when(announcementDao).page(any(), any());
        when(discussionPostDao.list()).thenReturn(Collections.emptyList());

        HomeDashboardVO vo = service.dashboard();

        assertEquals(Integer.valueOf(0), vo.getStats().getActiveUsers());
        assertTrue(vo.getQuickTasks().isEmpty(), "未登录不解析待办");
    }
}
