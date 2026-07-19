package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.RequirementConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.RequirementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * service 核心 CRUD 单测：角色门槛 / 发布人回填 / 归属校验 / 乐观锁 / 可见性 / 分类双写。
 *
 * <p>业务角色由 {@link CurrentUserResolver} 查本地 sys_user 判定，此处直接 mock 它，
 * 故多数用例不再需要 {@code mockStatic(SecurityUtils)}——只有 page/detail 的可见性
 * 过滤仍直接取 UAA userId。</p>
 */
class RequirementServiceImplTest {

    @Mock RequirementDao dao;
    @Mock CategoryDao categoryDao;
    @Mock RequestCategoryDao requestCategoryDao;
    @Mock CurrentUserResolver currentUser;
    @Spy  RequirementConvert convert = new RequirementConvert();
    @InjectMocks RequirementServiceImpl service;

    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    // —— fixtures ——

    private SysUserDO localUser(Long id, String name, String role) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        u.setStatus("active");
        u.setDepartmentId(1001L);
        u.setDepartmentName("上海销售组");
        return u;
    }

    /** 让鉴权放行，并返回给定档案。 */
    private void authorizedAs(SysUserDO me) {
        when(currentUser.requireAnyRole(anyString(), anyString(), anyString())).thenReturn(me);
    }

    private RequirementCreateDTO validCreate() {
        RequirementCreateDTO dto = new RequirementCreateDTO();
        dto.setTitle("t");
        dto.setDescription("d");
        dto.setIndustry("IoT");
        dto.setUrgency("normal");
        dto.setVisibilityType("all");
        return dto;
    }

    private RequirementUpdateDTO validUpdate(Long id, Integer version) {
        RequirementUpdateDTO dto = new RequirementUpdateDTO();
        dto.setId(id);
        dto.setVersion(version);
        dto.setTitle("t2");
        dto.setDescription("d");
        dto.setUrgency("normal");
        dto.setVisibilityType("all");
        return dto;
    }

    // —— 鉴权 ——

    @Test
    void create_rejects_when_role_not_allowed() {
        when(currentUser.requireAnyRole(anyString(), anyString(), anyString()))
                .thenThrow(new BaseException(ErrorCode.FORBIDDEN, "当前角色无权执行该操作"));

        assertThrows(BaseException.class, () -> service.create(validCreate()));
        verify(dao, never()).save(any());
    }

    @Test
    void create_backfills_publisher_from_local_profile() {
        authorizedAs(localUser(9002L, "张伟", CurrentUserResolver.ROLE_SALES));
        when(dao.save(any())).thenAnswer(inv -> {
            ((OpportunityRequestDO) inv.getArgument(0)).setId(1L);
            return true;
        });

        Long id = service.create(validCreate());

        assertEquals(Long.valueOf(1L), id);
        ArgumentCaptor<OpportunityRequestDO> cap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
        verify(dao).save(cap.capture());
        OpportunityRequestDO saved = cap.getValue();
        assertEquals(Long.valueOf(9002L), saved.getPublisherId(), "publisherId 须服务端回填，且等于 sys_user.id");
        assertEquals("张伟", saved.getPublisherName(), "发布人姓名取自本地档案");
        assertEquals("上海销售组", saved.getPublisherDeptName(), "部门取自本地档案（UAA 不提供部门）");
        assertEquals(Long.valueOf(1001L), saved.getDepartmentId());
        assertEquals("Pending", saved.getStatus(), "初态 Pending");
        assertEquals("L0", saved.getEscalationLevel());
        assertNull(saved.getCreateBy(), "审计字段禁业务赋值，由框架 MetaObjectHandler 填充");
    }

    // —— 乐观锁与归属 ——

    @Test
    void update_throws_version_conflict_when_updateById_false() {
        authorizedAs(localUser(9002L, "张伟", CurrentUserResolver.ROLE_SALES));
        OpportunityRequestDO existing = new OpportunityRequestDO();
        existing.setId(1L);
        existing.setPublisherId(9002L);
        existing.setVersion(3);
        when(dao.getById(1L)).thenReturn(existing);
        when(dao.updateById(any())).thenReturn(false);      // 影响 0 行 = 版本冲突

        BaseException ex = assertThrows(BaseException.class, () -> service.update(validUpdate(1L, 3)));
        assertTrue(ex.getMessage() != null && ex.getMessage().contains("已被他人修改"), ex.getMessage());
    }

    @Test
    void update_rejects_non_owner() {
        authorizedAs(localUser(9999L, "王强", CurrentUserResolver.ROLE_SALES));   // 非发布人
        OpportunityRequestDO existing = new OpportunityRequestDO();
        existing.setId(1L);
        existing.setPublisherId(9002L);
        existing.setVersion(1);
        when(dao.getById(1L)).thenReturn(existing);

        assertThrows(BaseException.class, () -> service.update(validUpdate(1L, 1)));
        verify(dao, never()).updateById(any());
    }

    @Test
    void update_allows_admin_on_others_requirement() {
        authorizedAs(localUser(9001L, "运营管理员", CurrentUserResolver.ROLE_ADMIN));
        OpportunityRequestDO existing = new OpportunityRequestDO();
        existing.setId(1L);
        existing.setPublisherId(9002L);      // 别人发布的
        existing.setVersion(1);
        when(dao.getById(1L)).thenReturn(existing);
        when(dao.updateById(any())).thenReturn(true);

        service.update(validUpdate(1L, 1));

        verify(dao).updateById(any());
    }

    // —— 参数校验 ——

    @Test
    void create_rejects_dept_scope_without_values() {
        authorizedAs(localUser(9002L, "张伟", CurrentUserResolver.ROLE_SALES));
        RequirementCreateDTO dto = validCreate();
        dto.setVisibilityType("dept");            // 未给 visibilityValues

        assertThrows(BaseException.class, () -> service.create(dto));
        verify(dao, never()).save(any());
    }

    @Test
    void create_rejects_non_numeric_category_id() {
        authorizedAs(localUser(9002L, "张伟", CurrentUserResolver.ROLE_SALES));
        RequirementCreateDTO dto = validCreate();
        dto.setCategoryIds(Arrays.asList("cat1", "5g"));   // 前端旧 mock 里的字符串 code

        assertThrows(BaseException.class, () -> service.create(dto),
                "非数字分类 id 必须显式报错，不能静默丢分类");
        verify(dao, never()).save(any());
    }

    @Test
    void detail_throws_when_not_found() {
        when(dao.getById(404L)).thenReturn(null);
        assertThrows(BaseException.class, () -> service.detail(404L));
    }

    // —— 分类快照 + 关联表双写 ——

    @Test
    void create_writes_category_snapshot_and_join_rows() {
        authorizedAs(localUser(9002L, "张伟", CurrentUserResolver.ROLE_SALES));
        CategoryDO c1 = new CategoryDO();
        c1.setId(101L);
        c1.setName("5G 模组");
        CategoryDO c2 = new CategoryDO();
        c2.setId(102L);
        c2.setName("NB-IoT 模组");
        when(categoryDao.listByIds(anyCollection())).thenReturn(Arrays.asList(c1, c2));
        when(dao.save(any())).thenAnswer(inv -> {
            ((OpportunityRequestDO) inv.getArgument(0)).setId(7L);
            return true;
        });
        when(requestCategoryDao.saveBatch(anyCollection())).thenReturn(true);

        RequirementCreateDTO dto = validCreate();
        dto.setCategoryIds(Arrays.asList("101", "102"));

        service.create(dto);

        ArgumentCaptor<OpportunityRequestDO> cap = ArgumentCaptor.forClass(OpportunityRequestDO.class);
        verify(dao).save(cap.capture());
        assertEquals(Arrays.asList("5G 模组", "NB-IoT 模组"), cap.getValue().getCategoryNames(),
                "categoryNames 快照须随主表落库");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<RequestCategoryDO>> join = ArgumentCaptor.forClass(Collection.class);
        verify(requestCategoryDao).saveBatch(join.capture());
        assertEquals(2, join.getValue().size(), "request_category 须写 2 行");
    }

    // —— 可见性（仍走 UAA userId）——

    @Test
    void detail_rejects_invisible_requirement() {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setId(5L);
        d.setVisibilityScope("dept");     // 非公开
        d.setPublisherId(9002L);
        when(dao.getById(5L)).thenReturn(d);

        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn(9999L);   // 非发布人
            assertThrows(BaseException.class, () -> service.detail(5L));
        }
    }
}
