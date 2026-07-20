package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionReplyDO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionDetailVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 讨论区契约测试：DTO→DO 映射 + 递归组树 + VO 离线序列化门禁。
 *
 * <p>序列化断言不必等联调：纯 ObjectMapper 就能钉死 snake_case 键名、日期格式、
 * 递归 children 结构与「不出现 camelCase」。</p>
 */
class DiscussionConvertTest {

    private final DiscussionConvert convert = new DiscussionConvert();

    @Test
    void toCreateDO_maps_payload_and_leaves_author_alone() {
        DiscussionCreateDTO dto = new DiscussionCreateDTO();
        dto.setTitle("RedCap 选型讨论");
        dto.setTopic("business");
        dto.setContent("<p>正文</p>");

        DiscussionPostDO d = convert.toCreateDO(dto);

        assertEquals("RedCap 选型讨论", d.getTitle());
        assertEquals("business", d.getTopic());
        assertEquals("<p>正文</p>", d.getContent());
        // author* 由 service 从本地 sys_user 回填，convert 不碰（防伪造）
        assertNull(d.getAuthorId());
        assertNull(d.getAuthorName());
    }

    @Test
    void buildCommentTree_nests_children_and_sorts_by_time() {
        // 一级 100（10:00）+ 其子 101（10:20）；另一根 102（09:00，时间更早应排前）
        DiscussionReplyDO root1 = reply(100L, null, "李娜", LocalDateTime.of(2026, 7, 10, 10, 0, 0));
        DiscussionReplyDO child = reply(101L, 100L, "张伟", LocalDateTime.of(2026, 7, 10, 10, 20, 0));
        DiscussionReplyDO root2 = reply(102L, null, "王强", LocalDateTime.of(2026, 7, 10, 9, 0, 0));

        List<DiscussionCommentVO> tree = convert.buildCommentTree(Arrays.asList(root1, child, root2));

        assertEquals(2, tree.size(), "两个根节点");
        // 根按 create_time 正序：102(09:00) 在前，100(10:00) 在后
        assertEquals(Long.valueOf(102L), tree.get(0).getCommentId());
        assertEquals(Long.valueOf(100L), tree.get(1).getCommentId());
        // 100 下挂着 101
        assertEquals(1, tree.get(1).getChildren().size());
        assertEquals(Long.valueOf(101L), tree.get(1).getChildren().get(0).getCommentId());
    }

    @Test
    void buildCommentTree_orphan_parent_promoted_to_root() {
        // 父引用 999 不在集合里（孤儿）→ 挂根，不丢弃
        DiscussionReplyDO orphan = reply(200L, 999L, "陈涛", LocalDateTime.of(2026, 7, 9, 15, 10, 0));

        List<DiscussionCommentVO> tree = convert.buildCommentTree(Collections.singletonList(orphan));

        assertEquals(1, tree.size(), "孤儿应提升为根节点而非丢失");
        assertEquals(Long.valueOf(200L), tree.get(0).getCommentId());
    }

    @Test
    void detailVO_serializes_snake_case_safe_date_and_recursive_children() throws Exception {
        DiscussionPostDO d = new DiscussionPostDO();
        d.setId(123456789012345678L);
        d.setTitle("RedCap 选型讨论");
        d.setTopic("business");
        d.setAuthorName("张伟");
        d.setReplyCount(18);
        d.setViewCount(642);
        d.setIsHot(true);
        d.setTags(Arrays.asList("5G", "工业网关"));
        d.setCreateTime(LocalDateTime.of(2026, 7, 10, 9, 12, 0));

        // 两层样例：一级 300（10:02）→ 子 301（10:20）
        DiscussionReplyDO r1 = reply(300L, null, "李娜", LocalDateTime.of(2026, 7, 10, 10, 2, 0));
        DiscussionReplyDO r2 = reply(301L, 300L, "张伟", LocalDateTime.of(2026, 7, 10, 10, 20, 0));

        DiscussionDetailVO vo = convert.toDetailVO(d, Arrays.asList(r1, r2));
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        // 帖字段 snake_case
        assertTrue(json.contains("\"post_id\""), json);
        assertTrue(json.contains("\"author_name\""), json);
        assertTrue(json.contains("\"reply_count\""), json);
        assertTrue(json.contains("\"view_count\""), json);
        assertTrue(json.contains("\"is_hot\":true"), json);
        assertFalse(json.contains("authorName"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("replyCount"), "不得出现 camelCase：" + json);
        // 递归回帖 snake_case + 嵌套 children
        assertTrue(json.contains("\"comment_id\""), json);
        assertTrue(json.contains("\"children\""), json);
        // 日期安全格式（禁 ISO 'T'）
        assertTrue(json.contains("\"2026-07-10 09:12:00\""), json);
        assertTrue(json.contains("\"2026-07-10 10:20:00\""), json);
        assertFalse(json.contains("2026-07-10T10:20"), "禁 ISO 'T' 分隔：" + json);

        // 结构断言：300 的 children 里含 301
        assertEquals(Long.valueOf(300L), vo.getComments().get(0).getCommentId());
        assertEquals(Long.valueOf(301L), vo.getComments().get(0).getChildren().get(0).getCommentId());
    }

    private DiscussionReplyDO reply(Long id, Long parentId, String author, LocalDateTime time) {
        DiscussionReplyDO r = new DiscussionReplyDO();
        r.setId(id);
        r.setParentId(parentId);
        r.setAuthorName(author);
        r.setContent("内容" + id);
        r.setCreateTime(time);
        return r;
    }
}
