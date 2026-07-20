package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.FeedbackConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.FeedbackDao;
import com.quectel.web.cloud.salesleadhubserver.dto.FeedbackCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.FeedbackDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.FeedbackServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 吐槽墙 service 行为测试：匿名昵称生成 + 点赞原子自增。dao 全 mock，离线可跑。
 */
class FeedbackServiceImplTest {

    @Mock FeedbackDao dao;
    @Mock CurrentUserResolver currentUser;

    private FeedbackServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new FeedbackServiceImpl(dao, currentUser, new FeedbackConvert());
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

    private FeedbackCreateDTO dto() {
        FeedbackCreateDTO d = new FeedbackCreateDTO();
        d.setTitle("样机申请流程太长了");
        d.setContent("走五六个审批节点");
        return d;
    }

    @Test
    void create_generates_anon_name_that_is_not_real_name() {
        SysUserDO me = user(9002L, "张伟");
        when(currentUser.requireAnyRole(any())).thenReturn(me);

        service.create(dto());

        ArgumentCaptor<FeedbackDO> cap = ArgumentCaptor.forClass(FeedbackDO.class);
        verify(dao).save(cap.capture());
        FeedbackDO saved = cap.getValue();
        // 匿名昵称非空、且不等于真实姓名（不可反推身份）
        assertNotNull(saved.getAnonName());
        assertTrue(saved.getAnonName().length() > 0, "匿名昵称须非空");
        assertNotEquals("张伟", saved.getAnonName(), "匿名昵称不得等于真实姓名");
        // emoji/color 由后端默认池回填
        assertNotNull(saved.getEmoji());
        assertNotNull(saved.getColor());
        assertTrue(saved.getLikeCount() == 0, "初始点赞数应为 0");
    }

    @Test
    void create_denied_propagates_and_never_saves() {
        when(currentUser.requireAnyRole(any()))
                .thenThrow(new BaseException(ErrorCode.FORBIDDEN, "未开通"));
        assertThrows(BaseException.class, () -> service.create(dto()));
        verify(dao, never()).save(any());
    }

    @Test
    void like_uses_atomic_increment() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, "张伟"));
        when(dao.increaseLikeCount(5001L)).thenReturn(true);

        service.like(5001L);

        // 必须走 dao 的原子自增，而非读出 +1 再写回（并发丢计数）
        verify(dao).increaseLikeCount(5001L);
        verify(dao, never()).updateById(any());
    }

    @Test
    void like_on_missing_row_raises_not_found() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, "张伟"));
        when(dao.increaseLikeCount(999L)).thenReturn(false);
        assertThrows(BaseException.class, () -> service.like(999L));
    }
}
