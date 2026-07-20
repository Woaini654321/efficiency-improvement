package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnounceDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnouncePageVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公告模块契约测试：DTO→DO 映射 + 前台/运营两套 VO 的离线序列化门禁。
 *
 * <p>纯 ObjectMapper 即可钉死 snake_case 键名、日期格式 yyyy-MM-dd HH:mm:ss、
 * 布尔 0/1 转 true/false、以及"不出现 camelCase"，不必等联调。</p>
 */
class AnnouncementConvertTest {

    private final AnnouncementConvert convert = new AnnouncementConvert();

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private AnnouncementDO sampleDO() {
        AnnouncementDO d = new AnnouncementDO();
        d.setId(123456789012345678L);
        d.setTitle("平台SLA首响时限规则说明");
        d.setContent("<p>特急2h/紧急4h/普通24h……</p>");
        d.setType("notice");
        d.setStatus("published");
        d.setPriority("high");
        d.setPublisherName("运营管理员");
        d.setIsPinned(1);
        d.setBannerEnabled(1);
        d.setViewCount(3020);
        d.setVersion(2);
        d.setCreateTime(LocalDateTime.of(2026, 7, 8, 8, 30, 0));
        d.setPublishedAt(LocalDateTime.of(2026, 7, 8, 9, 0, 0));
        return d;
    }

    @Test
    void toCreateDO_maps_payload_and_converts_bool_to_int_and_leaves_publisher_alone() {
        AnnounceCreateDTO dto = new AnnounceCreateDTO();
        dto.setTitle("新公告");
        dto.setType("policy");
        dto.setPriority("normal");
        dto.setContent("<p>正文</p>");
        dto.setIsPinned(true);
        dto.setBannerEnabled(false);

        AnnouncementDO d = convert.toCreateDO(dto);

        assertEquals("新公告", d.getTitle());
        assertEquals("policy", d.getType());
        assertEquals("normal", d.getPriority());
        assertEquals("<p>正文</p>", d.getContent());
        // 布尔转 0/1
        assertEquals(Integer.valueOf(1), d.getIsPinned());
        assertEquals(Integer.valueOf(0), d.getBannerEnabled());
        // status/publisher/publishedAt 由 service 控制，convert 不碰
        assertNull(d.getStatus());
        assertNull(d.getPublisherId());
        assertNull(d.getPublisherName());
        assertNull(d.getPublishedAt());
    }

    @Test
    void applyUpdate_carries_client_version_into_where_clause() {
        AnnounceUpdateDTO dto = new AnnounceUpdateDTO();
        dto.setId(1L);
        dto.setVersion(7);
        dto.setTitle("改后标题");
        dto.setType("notice");
        dto.setPriority("high");
        dto.setContent("<p>改后</p>");
        dto.setIsPinned(false);
        dto.setBannerEnabled(true);

        AnnouncementDO existing = new AnnouncementDO();
        existing.setVersion(9);   // 库里已是 9，客户端还拿着 7 的旧快照

        convert.applyUpdate(dto, existing);

        // 乐观锁靠实体 version 参与 WHERE version=?：必须回填客户端的 7
        assertEquals(Integer.valueOf(7), existing.getVersion());
        assertEquals("改后标题", existing.getTitle());
        assertEquals(Integer.valueOf(0), existing.getIsPinned());
        assertEquals(Integer.valueOf(1), existing.getBannerEnabled());
    }

    @Test
    void frontPageVO_serializes_snake_case_bool_and_safe_date() throws Exception {
        String json = mapper.writeValueAsString(convert.toFrontPageVO(sampleDO()));

        // 前端 adapter 读的是 announcement_id、publisher_name、view_count、published_at、is_pinned
        assertTrue(json.contains("\"announcement_id\""), json);
        assertTrue(json.contains("\"publisher_name\""), json);
        assertTrue(json.contains("\"view_count\""), json);
        assertTrue(json.contains("\"published_at\""), json);
        assertTrue(json.contains("\"is_pinned\":true"), json);
        assertFalse(json.contains("publisherName"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("viewCount"), "不得出现 camelCase：" + json);
        // 默认 ISO 的 'T' 会让前端 new Date(s.replace(/-/g,'/')) 解析出 NaN
        assertTrue(json.contains("\"2026-07-08 09:00:00\""), json);
        assertFalse(json.contains("2026-07-08T09:00"), "禁 ISO 'T' 分隔：" + json);
        // 前台列表窄契约：不下发 content
        assertFalse(json.contains("\"content\""), "前台列表不应下发 content：" + json);
    }

    @Test
    void frontDetailVO_includes_content_snake_case() throws Exception {
        String json = mapper.writeValueAsString(convert.toFrontDetailVO(sampleDO()));

        assertTrue(json.contains("\"announcement_id\""), json);
        assertTrue(json.contains("\"content\""), json);
        assertTrue(json.contains("特急2h"), json);
        assertFalse(json.contains("publisherName"), "不得出现 camelCase：" + json);
        // 前台契约无 version/banner_enabled
        assertFalse(json.contains("\"version\""), "前台详情不应含 version：" + json);
        assertFalse(json.contains("banner_enabled"), "前台详情不应含 banner_enabled：" + json);
    }

    @Test
    void opPageVO_serializes_full_contract_snake_case() throws Exception {
        String json = mapper.writeValueAsString(convert.toOpPageVO(sampleDO()));

        assertTrue(json.contains("\"announcement_id\""), json);
        assertTrue(json.contains("\"created_at\""), json);
        assertTrue(json.contains("\"published_at\""), json);
        assertTrue(json.contains("\"banner_enabled\":true"), json);
        assertTrue(json.contains("\"is_pinned\":true"), json);
        assertTrue(json.contains("\"version\":2"), json);
        // 运营列表需要 content 供预览抽屉直接读
        assertTrue(json.contains("\"content\""), json);
        // 时间格式安全
        assertTrue(json.contains("\"2026-07-08 08:30:00\""), json);
        assertFalse(json.contains("2026-07-08T08:30"), "禁 ISO 'T' 分隔：" + json);
        assertFalse(json.contains("bannerEnabled"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("createdAt"), "不得出现 camelCase：" + json);
    }

    @Test
    void opDetailVO_serializes_full_contract_snake_case() throws Exception {
        String json = mapper.writeValueAsString(convert.toOpDetailVO(sampleDO()));

        assertTrue(json.contains("\"announcement_id\""), json);
        assertTrue(json.contains("\"content\""), json);
        assertTrue(json.contains("\"version\":2"), json);
        assertTrue(json.contains("\"banner_enabled\":true"), json);
        assertFalse(json.contains("createdAt"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("publishedAt"), "不得出现 camelCase：" + json);
    }

    @Test
    void pinned_false_serializes_as_boolean_false_not_zero() throws Exception {
        AnnouncementDO d = sampleDO();
        d.setIsPinned(0);
        d.setBannerEnabled(0);
        String json = mapper.writeValueAsString(convert.toOpPageVO(d));
        // TINYINT 0/1 必须转成 JSON 布尔（前端 a-switch 绑定的是 boolean）
        assertTrue(json.contains("\"is_pinned\":false"), json);
        assertTrue(json.contains("\"banner_enabled\":false"), json);
    }
}
