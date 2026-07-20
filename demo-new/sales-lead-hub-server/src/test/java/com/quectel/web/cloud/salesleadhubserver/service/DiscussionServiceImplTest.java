package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.DiscussionConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionPostDao;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionReplyDao;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionReplyDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionReplyDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.DiscussionServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionDetailVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 讨论区 service 行为测试：鉴权边界、话题枚举校验、发布人快照、detail 组树与 NOT_FOUND。
 * dao 全 mock，离线可跑（真库行为由 DiscussionIntegrationTest 覆盖）。
 */
class DiscussionServiceImplTest {

    @Mock DiscussionPostDao postDao;
    @Mock DiscussionReplyDao replyDao;
    @Mock CurrentUserResolver currentUser;

    private DiscussionServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new DiscussionServiceImpl(postDao, replyDao, currentUser, new DiscussionConvert());
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysUserDO user(long id, String role) {
        SysUserDO u = new SysUserDO();
        u.setId(id);
        u.setName("用户" + id);
        u.setRole(role);
        u.setStatus("active");
        return u;
    }

    private DiscussionCreateDTO createDto() {
        DiscussionCreateDTO dto = new DiscussionCreateDTO();
        dto.setTitle("RedCap 选型讨论");
        dto.setTopic("business");
        dto.setContent("<p>正文</p>");
        return dto;
    }

    // ---------- create ----------

    @Test
    void create_denied_propagates_and_never_saves() {
        when(currentUser.requireAnyRole(any()))
                .thenThrow(new BaseException(ErrorCode.FORBIDDEN, "拒绝"));
        assertThrows(BaseException.class, () -> service.create(createDto()));
        verify(postDao, never()).save(any());
    }

    @Test
    void create_snapshots_author_from_sys_user_and_defaults_counts() {
        SysUserDO me = user(9002L, CurrentUserResolver.ROLE_SALES);
        when(currentUser.requireAnyRole(any())).thenReturn(me);

        service.create(createDto());

        ArgumentCaptor<DiscussionPostDO> cap = ArgumentCaptor.forClass(DiscussionPostDO.class);
        verify(postDao).save(cap.capture());
        DiscussionPostDO saved = cap.getValue();
        // 作者快照必须取自本地 sys_user 同一行，不信客户端
        assertEquals(Long.valueOf(9002L), saved.getAuthorId());
        assertEquals("用户9002", saved.getAuthorName());
        assertEquals(Integer.valueOf(0), saved.getReplyCount());
        assertEquals(Integer.valueOf(0), saved.getViewCount());
        assertEquals(Boolean.FALSE, saved.getIsHot());
        assertEquals(0, saved.getTags().size(), "前端不采集 tags，落空数组");
    }

    @Test
    void create_invalid_topic_is_rejected() {
        when(currentUser.requireAnyRole(any()))
                .thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        DiscussionCreateDTO dto = createDto();
        dto.setTopic("gossip");   // 不在枚举内
        assertThrows(BaseException.class, () -> service.create(dto));
        verify(postDao, never()).save(any());
    }

    // ---------- detail ----------

    @Test
    void detail_not_found_raises() {
        when(postDao.getById(1L)).thenReturn(null);
        BaseException ex = assertThrows(BaseException.class, () -> service.detail(1L));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        // 不存在时不应再自增浏览数
        verify(postDao, never()).increaseViewCount(anyLong());
    }

    @Test
    void detail_increments_view_and_assembles_tree_with_orphan_at_root() {
        DiscussionPostDO d = new DiscussionPostDO();
        d.setId(1L);
        d.setTitle("帖");
        d.setTopic("business");
        d.setViewCount(642);
        when(postDao.getById(1L)).thenReturn(d);
        when(postDao.increaseViewCount(1L)).thenReturn(true);

        // 一级 300 → 子 301；另有孤儿 302（父 999 不存在）
        DiscussionReplyDO r1 = reply(300L, null, LocalDateTime.of(2026, 7, 10, 10, 0, 0));
        DiscussionReplyDO r2 = reply(301L, 300L, LocalDateTime.of(2026, 7, 10, 10, 20, 0));
        DiscussionReplyDO orphan = reply(302L, 999L, LocalDateTime.of(2026, 7, 10, 9, 0, 0));
        when(replyDao.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(Arrays.asList(r1, r2, orphan));

        DiscussionDetailVO vo = service.detail(1L);

        verify(postDao).increaseViewCount(1L);
        // 返回体现自增后的浏览数
        assertEquals(Integer.valueOf(643), vo.getViewCount());
        // 组树：300 与孤儿 302 都在根（302 时间更早排前），300 挂着 301
        assertEquals(2, vo.getComments().size());
        assertEquals(Long.valueOf(302L), vo.getComments().get(0).getCommentId());
        assertEquals(Long.valueOf(300L), vo.getComments().get(1).getCommentId());
        assertEquals(Long.valueOf(301L), vo.getComments().get(1).getChildren().get(0).getCommentId());
    }

    private DiscussionReplyDO reply(Long id, Long parentId, LocalDateTime time) {
        DiscussionReplyDO r = new DiscussionReplyDO();
        r.setId(id);
        r.setParentId(parentId);
        r.setAuthorName("作者" + id);
        r.setContent("内容" + id);
        r.setCreateTime(time);
        return r;
    }

    // ---------- reply（回帖持久化） ----------

    private DiscussionReplyDTO replyDto(Long postId, Long parentId) {
        DiscussionReplyDTO dto = new DiscussionReplyDTO();
        dto.setPostId(postId);
        dto.setParentId(parentId);
        dto.setContent("<p>回帖内容</p>");
        return dto;
    }

    @Test
    void reply_post_not_found_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        when(postDao.getById(500L)).thenReturn(null);

        BaseException ex = assertThrows(BaseException.class, () -> service.reply(replyDto(500L, null)));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        // 帖不存在：既不落库也不自增计数
        verify(replyDao, never()).save(any());
        verify(postDao, never()).increaseReplyCount(anyLong());
    }

    @Test
    void reply_parent_from_other_post_rejected() {
        when(currentUser.requireAnyRole(any())).thenReturn(user(9002L, CurrentUserResolver.ROLE_SALES));
        DiscussionPostDO post = new DiscussionPostDO();
        post.setId(500L);
        when(postDao.getById(500L)).thenReturn(post);
        // 父回帖存在但属于别的帖（999）→ 跨帖挂树，必须拒绝
        DiscussionReplyDO parent = new DiscussionReplyDO();
        parent.setId(700L);
        parent.setPostId(999L);
        when(replyDao.getById(700L)).thenReturn(parent);

        assertThrows(BaseException.class, () -> service.reply(replyDto(500L, 700L)));
        verify(replyDao, never()).save(any());
        verify(postDao, never()).increaseReplyCount(anyLong());
    }

    @Test
    void reply_persists_snapshots_author_and_increments_reply_count_atomically() {
        SysUserDO me = user(9002L, CurrentUserResolver.ROLE_SALES);
        when(currentUser.requireAnyRole(any())).thenReturn(me);
        DiscussionPostDO post = new DiscussionPostDO();
        post.setId(500L);
        when(postDao.getById(500L)).thenReturn(post);
        // 父回帖属于同一帖（500）→ 合法
        DiscussionReplyDO parent = new DiscussionReplyDO();
        parent.setId(700L);
        parent.setPostId(500L);
        when(replyDao.getById(700L)).thenReturn(parent);

        DiscussionCommentVO node = service.reply(replyDto(500L, 700L));

        ArgumentCaptor<DiscussionReplyDO> cap = ArgumentCaptor.forClass(DiscussionReplyDO.class);
        verify(replyDao).save(cap.capture());
        DiscussionReplyDO saved = cap.getValue();
        assertEquals(Long.valueOf(500L), saved.getPostId());
        assertEquals(Long.valueOf(700L), saved.getParentId());
        // 作者快照必须取自本地 sys_user 同一行，不信客户端
        assertEquals(Long.valueOf(9002L), saved.getAuthorId());
        assertEquals("用户9002", saved.getAuthorName());
        // create_time 不由 service 手赋（DO 上 fill=INSERT，由框架回填）
        assertNull(saved.getCreateTime());
        // reply_count 原子 +1（同事务）
        verify(postDao).increaseReplyCount(500L);
        // 返回新节点：children 空数组、内容回显
        assertEquals(0, node.getChildren().size());
        assertEquals("<p>回帖内容</p>", node.getContent());
    }
}
