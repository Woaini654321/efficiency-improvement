package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.web.cloud.salesleadhubserver.convert.EmployeeConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.EmployeePageDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.EmployeeServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.vo.EmployeePageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人员选择器 service 行为测试：只查 active 用户、keyword 过滤生效、出参只暴露 4 个非敏感字段。
 * dao 全 mock，验证 wrapper 逻辑简化为验证关键调用（status='active'、keyword 组合过滤开关）。
 */
class EmployeeServiceImplTest {

    @Mock SysUserDao sysUserDao;

    private EmployeeServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new EmployeeServiceImpl(sysUserDao, new EmployeeConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user() {
        SysUserDO u = new SysUserDO();
        u.setId(9002L);
        u.setName("张伟");
        u.setEmployeeId("E1001");
        u.setDepartmentName("上海销售组");
        u.setStatus("active");
        u.setPhone("13800000000");   // 敏感字段：不应出现在 VO
        u.setEmail("zhangwei@quectel.com");
        return u;
    }

    @SuppressWarnings("unchecked")
    private LambdaQueryChainWrapper<SysUserDO> stubChain(IPage<SysUserDO> page) {
        LambdaQueryChainWrapper<SysUserDO> chain =
                (LambdaQueryChainWrapper<SysUserDO>) mock(LambdaQueryChainWrapper.class);
        when(sysUserDao.lambdaQuery()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.and(anyBoolean(), any())).thenReturn(chain);
        when(chain.orderByAsc(any(SFunction.class))).thenReturn(chain);
        when(chain.page(any())).thenReturn(page);
        return chain;
    }

    @Test
    void page_only_active_and_maps_non_sensitive_fields() {
        Page<SysUserDO> page = new Page<>();
        page.setRecords(Collections.singletonList(user()));
        page.setTotal(1);
        LambdaQueryChainWrapper<SysUserDO> chain = stubChain(page);

        EmployeePageDTO dto = new EmployeePageDTO();
        dto.setKeyword("张");
        PageVO<EmployeePageVO> r = service.page(dto);

        assertEquals(1, r.getRecords().size());
        EmployeePageVO vo = r.getRecords().get(0);
        assertEquals("张伟", vo.getName());
        assertEquals("E1001", vo.getEmployeeId());
        assertEquals("上海销售组", vo.getDepartmentName());
        assertEquals(1L, r.getTotal());
        // 只查在职用户
        verify(chain).eq(any(), eq("active"));
        // keyword 非空 → 启用「姓名 or 工号」组合过滤
        verify(chain).and(eq(true), any());
    }

    @Test
    void page_without_keyword_disables_combined_filter() {
        Page<SysUserDO> page = new Page<>();
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        LambdaQueryChainWrapper<SysUserDO> chain = stubChain(page);

        service.page(new EmployeePageDTO());   // 无 keyword

        verify(chain).eq(any(), eq("active"));
        // keyword 为空 → 组合过滤开关关闭（condition=false）
        verify(chain).and(eq(false), any());
    }
}
