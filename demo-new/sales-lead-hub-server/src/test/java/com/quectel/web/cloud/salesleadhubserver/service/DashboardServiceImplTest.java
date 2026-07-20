package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.convert.DashboardConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ViewLogDao;
import com.quectel.web.cloud.salesleadhubserver.dto.DashboardQueryDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.DashboardServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 运营看板 service 行为测试：环比 prev=0→0、除 0 防护、时段分桶长度 24、趋势序列粒度长度。
 * 取数走 IService.list()，全部可 mock；构造「仅当前周期有数据」使上一周期为 0。
 */
class DashboardServiceImplTest {

    @Mock OpportunityDao opportunityDao;
    @Mock RequirementDao requirementDao;
    @Mock ViewLogDao viewLogDao;
    @Mock OpportunityCategoryDao opportunityCategoryDao;
    @Mock RequestCategoryDao requestCategoryDao;
    @Mock CategoryDao categoryDao;
    @Mock CurrentUserResolver currentUser;

    private DashboardServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new DashboardServiceImpl(opportunityDao, requirementDao, viewLogDao,
                opportunityCategoryDao, requestCategoryDao, categoryDao, currentUser, new DashboardConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private ViewLogDO view(long userId, LocalDateTime at) {
        ViewLogDO v = new ViewLogDO();
        v.setUserId(userId);
        v.setTargetType("Opportunity");
        v.setViewedAt(at);
        return v;
    }

    @Test
    void mom_zero_when_no_previous_period_and_guards_and_bucket_lengths() {
        SysUserDO admin = new SysUserDO();
        admin.setRole(CurrentUserResolver.ROLE_ADMIN);
        when(currentUser.requireAnyRole(any())).thenReturn(admin);

        LocalDateTime now = LocalDateTime.now();
        // 全部落在当前 7 天周期内，上一周期为空
        when(viewLogDao.list()).thenReturn(Arrays.asList(view(1L, now.minusHours(1)), view(2L, now.minusHours(2))));
        OpportunityDO opp = new OpportunityDO();
        opp.setStatus("published");
        opp.setCreateTime(now.minusDays(1));
        opp.setViewCount(100);
        when(opportunityDao.list()).thenReturn(Collections.singletonList(opp));
        OpportunityRequestDO req = new OpportunityRequestDO();
        req.setStatus("Pending");
        req.setResponseCount(0);
        req.setCreateTime(now.minusDays(1));
        when(requirementDao.list()).thenReturn(Collections.singletonList(req));
        when(opportunityCategoryDao.list()).thenReturn(Collections.emptyList());
        when(requestCategoryDao.list()).thenReturn(Collections.emptyList());
        when(categoryDao.list()).thenReturn(Collections.emptyList());

        DashboardVO vo = service.dashboard(new DashboardQueryDTO());

        // UV/PV
        assertEquals(Integer.valueOf(2), vo.getUv());
        assertEquals(Integer.valueOf(2), vo.getPv());
        // 上一周期无数据 → 环比 0（不造假）
        assertEquals(0.0, vo.getUvMom());
        assertEquals(0.0, vo.getPvMom());
        assertEquals(0.0, vo.getWeekPublishMom());
        // 周发布 = 1 商机 + 1 需求
        assertEquals(Integer.valueOf(2), vo.getWeekPublish());
        // 响应率除 0 防护：1 条需求、0 响应 → 0.0（非 NaN）
        assertEquals(0.0, vo.getResponseRate());
        assertEquals(0.0, vo.getResponseRateMom());

        // 时段分桶固定 24
        assertEquals(24, vo.getHourlyActive().size());
        assertEquals(2, vo.getHourlyActive().stream().mapToInt(Integer::intValue).sum(), "两条浏览应落在近 24h 桶内");

        // 趋势四粒度长度
        assertEquals(7, vo.getWeekPublishTrend().getLast7d().size());
        assertEquals(4, vo.getWeekPublishTrend().getLast4w().size());
        assertEquals(12, vo.getWeekPublishTrend().getLast12w().size());
        assertEquals(6, vo.getWeekPublishTrend().getLast6m().size());
        assertEquals(7, vo.getResponseRateTrend().getLast7d().size());
    }
}
