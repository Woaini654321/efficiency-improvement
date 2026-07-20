package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.vo.CommentVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 互动模块契约测试：评论树 VO 离线序列化门禁 + 软删占位转换。
 *
 * <p>纯 ObjectMapper 就能钉死 snake_case 键名、日期格式与「不出现 camelCase」，不必等联调。</p>
 */
class InteractionConvertTest {

    private final InteractionConvert convert = new InteractionConvert();

    private InteractionDO comment(long id, Long parentId, String content, int deleted) {
        InteractionDO d = new InteractionDO();
        d.setId(id);
        d.setUserId(9002L);
        d.setUserName("张伟");
        d.setTargetType("Opportunity");
        d.setTargetId(1001L);
        d.setType("comment");
        d.setContent(content);
        d.setParentCommentId(parentId);
        d.setContentDeleted(deleted);
        d.setCreateTime(LocalDateTime.of(2026, 6, 21, 9, 30, 0));
        return d;
    }

    @Test
    void commentVO_serializes_snake_case_safe_date_and_nested_replies() throws Exception {
        CommentVO parent = convert.toCommentVO(comment(2001L, null, "功耗是实测还是标称？", 0), "无线模组产品部", 12);
        CommentVO reply = convert.toCommentVO(comment(2002L, 2001L, "实验室实测", 0), "行业解决方案部", 5);
        parent.setReplies(Collections.singletonList(reply));

        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(parent);

        // 前端 adapter 读的键名
        assertTrue(json.contains("\"interaction_id\""), json);
        assertTrue(json.contains("\"target_type\""), json);
        assertTrue(json.contains("\"author_name\""), json);
        assertTrue(json.contains("\"author_dept\""), json);
        assertTrue(json.contains("\"like_count\""), json);
        assertTrue(json.contains("\"parent_id\""), json);
        assertTrue(json.contains("\"created_at\""), json);
        assertTrue(json.contains("\"replies\""), json);
        // 不得出现 camelCase
        assertFalse(json.contains("interactionId"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("authorName"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("likeCount"), "不得出现 camelCase：" + json);
        // 日期格式：禁 ISO 'T' 分隔（会让前端 replace(/-/g,'/') 解析出 NaN）
        assertTrue(json.contains("\"2026-06-21 09:30:00\""), json);
        assertFalse(json.contains("2026-06-21T09:30"), "禁 ISO 'T' 分隔：" + json);
        // 嵌套回复的内容确实下发
        assertTrue(json.contains("实验室实测"), json);
    }

    @Test
    void deleted_comment_content_becomes_placeholder() {
        CommentVO v = convert.toCommentVO(comment(2003L, null, "原始内容不该出现", 1), "无线模组产品部", 0);
        // content_deleted=1：内容转占位，行本身保留
        assertEquals(InteractionConvert.DELETED_PLACEHOLDER, v.getContent());
        assertEquals("该评论已删除", v.getContent());
    }

    @Test
    void normal_comment_keeps_original_content() {
        CommentVO v = convert.toCommentVO(comment(2004L, null, "正常内容", 0), "短距产品部", 3);
        assertEquals("正常内容", v.getContent());
    }
}
