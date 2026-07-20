package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.IntelConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CompetitorIntelDao;
import com.quectel.web.cloud.salesleadhubserver.dao.IndustryIntelDao;
import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelSubmitDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CompetitorIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.IntelServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 情报中心 service 行为测试：submit 快照取自本地 sys_user；detail 不存在抛 NOT_FOUND。
 * dao 全 mock，离线可跑（真库行为由 IntelIntegrationTest 覆盖）。
 */
class IntelServiceImplTest {

    @Mock CompetitorIntelDao competitorDao;
    @Mock IndustryIntelDao industryDao;
    @Mock CurrentUserResolver currentUser;

    private IntelServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new IntelServiceImpl(competitorDao, industryDao, currentUser, new IntelConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(long id, String name) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName(name);
        u.setRole(CurrentUserResolver.ROLE_SALES);
        u.setStatus("active");
        return u;
    }

    private CompetitorIntelSubmitDTO submitDto() {
        CompetitorIntelSubmitDTO dto = new CompetitorIntelSubmitDTO();
        dto.setBrand("Telit");
        dto.setSource("Telit 官网新闻");
        dto.setTitle("Telit 发布 FN990");
        dto.setContent("<p>正文</p>");
        return dto;
    }

    @Test
    void submit_snapshots_submitter_from_local_sys_user() {
        SysUserDO me = user(9002L, "张伟");
        when(currentUser.requireAnyRole(any())).thenReturn(me);

        service.submitCompetitor(submitDto());

        ArgumentCaptor<CompetitorIntelDO> cap = ArgumentCaptor.forClass(CompetitorIntelDO.class);
        verify(competitorDao).save(cap.capture());
        CompetitorIntelDO saved = cap.getValue();
        // 提交人快照必须来自当前登录人的本地档案，不取前端传入
        assertEquals(Long.valueOf(9002L), saved.getSubmitterId());
        assertEquals("张伟", saved.getSubmitterName());
        // content 落 overview 列；计数初值 0
        assertEquals("<p>正文</p>", saved.getOverview());
        assertEquals(Integer.valueOf(0), saved.getViewCount());
    }

    @Test
    void submit_denied_propagates_and_never_saves() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(com.quectel.code.web.exception.ErrorCode.FORBIDDEN, "未开通"));
        assertThrows(BaseException.class, () -> service.submitCompetitor(submitDto()));
        verify(competitorDao, never()).save(any());
    }

    @Test
    void competitor_detail_not_found_raises() {
        when(competitorDao.getById(999L)).thenReturn(null);
        assertThrows(BaseException.class, () -> service.competitorDetail(999L));
        // 不存在时不应误触发浏览自增
        verify(competitorDao, never()).increaseViewCount(any());
    }

    @Test
    void competitor_detail_increments_view_count() {
        CompetitorIntelDO d = new CompetitorIntelDO();
        d.setId(1L);
        d.setBrand("Telit");
        d.setViewCount(10);
        when(competitorDao.getById(1L)).thenReturn(d);
        when(competitorDao.increaseViewCount(1L)).thenReturn(true);

        assertEquals(Integer.valueOf(11), service.competitorDetail(1L).getViewCount());
        verify(competitorDao).increaseViewCount(1L);
    }

    @Test
    void industry_detail_not_found_raises() {
        when(industryDao.getById(999L)).thenReturn(null);
        assertThrows(BaseException.class, () -> service.industryDetail(999L));
        verify(industryDao, never()).increaseViewCount(any());
    }
}
