package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.dto.FeedbackCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.FeedbackDO;
import com.quectel.web.cloud.salesleadhubserver.vo.FeedbackVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 吐槽墙契约测试：DTO→DO 映射 + VO 离线序列化门禁。
 *
 * <p>纯 ObjectMapper 就能钉死 snake_case 键名、日期格式、以及<b>绝不出现 create_by</b>
 * （匿名产品红线）。</p>
 */
class FeedbackConvertTest {

    private final FeedbackConvert convert = new FeedbackConvert();

    @Test
    void toCreateDO_maps_only_title_and_content() {
        FeedbackCreateDTO dto = new FeedbackCreateDTO();
        dto.setTitle("样机申请流程太长了");
        dto.setContent("走五六个审批节点");

        FeedbackDO d = convert.toCreateDO(dto);

        assertEquals("样机申请流程太长了", d.getTitle());
        assertEquals("走五六个审批节点", d.getContent());
        // anon_name/emoji/color/like_count 由 service 回填，convert 不碰
    }

    @Test
    void feedbackVO_serializes_snake_case_safe_date_and_never_leaks_create_by() throws Exception {
        FeedbackDO d = new FeedbackDO();
        d.setId(972010000000000001L);
        d.setTitle("规格书版本太乱");
        d.setContent("求统一出口");
        d.setAnonName("爱吐槽的水獭");
        d.setEmoji("🤯");
        d.setColor("#fa8c16");
        d.setLikeCount(96);
        d.setCreateTime(LocalDateTime.of(2026, 7, 14, 16, 40, 0));
        d.setCreateBy(9002L);   // 真实作者：绝不能被序列化出去

        FeedbackVO vo = convert.toVO(d);
        String json = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .writeValueAsString(vo);

        // 前端 adapter 读的是 feedback_id、anon_name、like_count、created_at
        assertTrue(json.contains("\"feedback_id\""), json);
        assertTrue(json.contains("\"anon_name\""), json);
        assertTrue(json.contains("\"like_count\""), json);
        assertTrue(json.contains("\"created_at\""), json);
        assertFalse(json.contains("anonName"), "不得出现 camelCase：" + json);
        // 匿名红线：真实作者字段绝不出现
        assertFalse(json.contains("create_by"), "禁泄真实作者 create_by：" + json);
        assertFalse(json.contains("createBy"), "禁泄真实作者 createBy：" + json);
        // 默认 ISO 的 'T' 分隔会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
        assertTrue(json.contains("\"2026-07-14 16:40:00\""), json);
        assertFalse(json.contains("2026-07-14T16:40"), "禁 ISO 'T' 分隔：" + json);
    }
}
