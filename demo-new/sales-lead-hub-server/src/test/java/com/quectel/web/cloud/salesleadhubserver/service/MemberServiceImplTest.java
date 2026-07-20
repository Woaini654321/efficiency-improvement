package com.quectel.web.cloud.salesleadhubserver.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.MemberConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberAddDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 产品线成员 service 行为测试：写操作鉴权、owner 唯一校验、user_name 快照来源、
 * 重复成员唯一键冲突转业务码。dao 全 mock，离线可跑（真库行为由 MemberIntegrationTest 覆盖）。
 */
class MemberServiceImplTest {

    @Mock ProductLineMemberDao memberDao;
    @Mock ProductLineDao productLineDao;
    @Mock SysUserDao sysUserDao;
    @Mock CurrentUserResolver currentUser;

    private MemberServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new MemberServiceImpl(memberDao, productLineDao, sysUserDao,
                currentUser, new MemberConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(long id, String name, String status) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName(name);
        u.setStatus(status);
        return u;
    }

    private ProductLineDO line(long id) {
        ProductLineDO l = new ProductLineDO();
        l.setId(id);
        l.setName("无线模组产品线");
        return l;
    }

    private SysUserDO admin() {
        return user(9001L, "运营管理员", "active");
    }

    /** 让 memberDao.lambdaQuery()...exists() 返回指定值（owner 唯一校验的数据条件）。 */
    @SuppressWarnings("unchecked")
    private void stubOwnerExists(boolean exists) {
        LambdaQueryChainWrapper<ProductLineMemberDO> chain = (LambdaQueryChainWrapper<ProductLineMemberDO>)
                org.mockito.Mockito.mock(LambdaQueryChainWrapper.class);
        when(memberDao.lambdaQuery()).thenReturn(chain);
        when(chain.eq(any(), any())).thenReturn(chain);
        when(chain.ne(anyBoolean(), any(), any())).thenReturn(chain);
        when(chain.exists()).thenReturn(exists);
    }

    private MemberAddDTO addDto(String plId, String uid, Integer isOwner) {
        MemberAddDTO dto = new MemberAddDTO();
        dto.setProductLineId(plId);
        dto.setUserId(uid);
        dto.setIsOwner(isOwner);
        return dto;
    }

    // ---------- add ----------

    @Test
    void add_denied_for_non_admin_never_saves() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        assertThrows(BaseException.class, () -> service.add(addDto("501", "9002", 0)));
        verify(memberDao, never()).save(any());
    }

    @Test
    void add_snapshots_userName_from_local_sysUser() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        when(productLineDao.getById(501L)).thenReturn(line(501L));
        when(sysUserDao.getById(9002L)).thenReturn(user(9002L, "张伟", "active"));

        service.add(addDto("501", "9002", 0));   // 非 owner，不触发唯一校验

        ArgumentCaptor<ProductLineMemberDO> cap = ArgumentCaptor.forClass(ProductLineMemberDO.class);
        verify(memberDao).save(cap.capture());
        ProductLineMemberDO saved = cap.getValue();
        // user_name 必须是本地 sys_user 的姓名，不接受客户端传入
        assertEquals("张伟", saved.getUserName());
        assertEquals(Long.valueOf(501L), saved.getProductLineId());
        assertEquals(Long.valueOf(9002L), saved.getUserId());
    }

    @Test
    void add_inactive_user_is_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        when(productLineDao.getById(501L)).thenReturn(line(501L));
        when(sysUserDao.getById(9002L)).thenReturn(user(9002L, "张伟", "disabled"));

        assertThrows(BaseException.class, () -> service.add(addDto("501", "9002", 0)));
        verify(memberDao, never()).save(any());
    }

    @Test
    void add_second_owner_is_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        when(productLineDao.getById(501L)).thenReturn(line(501L));
        when(sysUserDao.getById(9002L)).thenReturn(user(9002L, "张伟", "active"));
        stubOwnerExists(true);   // 该产品线已有其他 owner

        assertThrows(BaseException.class, () -> service.add(addDto("501", "9002", 1)));
        verify(memberDao, never()).save(any());
    }

    @Test
    void add_duplicate_member_maps_to_business_code() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        when(productLineDao.getById(501L)).thenReturn(line(501L));
        when(sysUserDao.getById(9002L)).thenReturn(user(9002L, "张伟", "active"));
        // 命中 uk_plm_line_user：DAO 抛 DuplicateKeyException，service 须转业务码
        when(memberDao.save(any())).thenThrow(
                new org.springframework.dao.DuplicateKeyException("uk_plm_line_user"));

        assertThrows(BaseException.class, () -> service.add(addDto("501", "9002", 0)));
    }

    // ---------- update ----------

    @Test
    void update_denied_for_non_admin_never_updates() {
        when(currentUser.requireAnyRole(any())).thenThrow(
                new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        MemberUpdateDTO dto = new MemberUpdateDTO();
        dto.setId(601L);
        dto.setIsOwner(1);
        assertThrows(BaseException.class, () -> service.update(dto));
        verify(memberDao, never()).updateById(any());
    }

    @Test
    void update_set_owner_when_another_owner_exists_is_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        ProductLineMemberDO existing = new ProductLineMemberDO();
        existing.setId(602L);
        existing.setProductLineId(501L);
        existing.setUserId(9002L);
        existing.setIsOwner(0);
        when(memberDao.getById(602L)).thenReturn(existing);
        stubOwnerExists(true);   // 排除自身后仍有其他 owner

        MemberUpdateDTO dto = new MemberUpdateDTO();
        dto.setId(602L);
        dto.setIsOwner(1);
        assertThrows(BaseException.class, () -> service.update(dto));
        verify(memberDao, never()).updateById(any());
    }

    @Test
    void update_set_owner_when_none_exists_succeeds() {
        when(currentUser.requireAnyRole(any())).thenReturn(admin());
        ProductLineMemberDO existing = new ProductLineMemberDO();
        existing.setId(602L);
        existing.setProductLineId(501L);
        existing.setUserId(9002L);
        existing.setIsOwner(0);
        when(memberDao.getById(602L)).thenReturn(existing);
        stubOwnerExists(false);   // 无其他 owner

        MemberUpdateDTO dto = new MemberUpdateDTO();
        dto.setId(602L);
        dto.setIsOwner(1);
        service.update(dto);

        ArgumentCaptor<ProductLineMemberDO> cap = ArgumentCaptor.forClass(ProductLineMemberDO.class);
        verify(memberDao).updateById(cap.capture());
        assertEquals(Integer.valueOf(1), cap.getValue().getIsOwner());
    }
}
