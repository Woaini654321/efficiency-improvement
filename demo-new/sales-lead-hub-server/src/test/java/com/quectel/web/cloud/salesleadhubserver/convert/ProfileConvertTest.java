package com.quectel.web.cloud.salesleadhubserver.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SolutionResponseDO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCollectVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfilePublishReplyVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfilePublishVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileSolutionVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 个人中心五段列表单行映射的契约测试：键名 snake_case、类型/采纳派生、富文本派生 title/summary、
 * 日期安全格式，纯离线（无 mock）。
 */
class ProfileConvertTest {

    private final ProfileConvert convert = new ProfileConvert();
    private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void collectVO_serializes_snake_case_and_safe_date() throws Exception {
        InteractionDO i = new InteractionDO();
        i.setId(7001L);
        i.setTargetId(1001L);
        i.setCreateTime(LocalDateTime.of(2026, 7, 16, 10, 20, 0));

        ProfileCollectVO v = convert.toCollectVO(i, "opportunity", "5G RedCap 选型方案");
        assertEquals(Long.valueOf(7001L), v.getCollectId());
        assertEquals("opportunity", v.getType());
        assertEquals(Boolean.FALSE, v.getIsDeleted());

        String json = om.writeValueAsString(v);
        assertTrue(json.contains("\"collect_id\""), json);
        assertTrue(json.contains("\"is_deleted\""), json);
        assertTrue(json.contains("\"2026-07-16 10:20:00\""), json);
        assertFalse(json.contains("collectId"), "不得出现 camelCase：" + json);
        assertFalse(json.contains("2026-07-16T10:20"), "禁 ISO 'T' 分隔：" + json);
    }

    @Test
    void publishVO_opportunity_is_not_adopted_and_snake_case() throws Exception {
        OpportunityDO o = new OpportunityDO();
        o.setId(1001L);
        o.setTitle("商机标题");
        o.setStatus("published");
        o.setViewCount(1280);
        o.setLikeCount(86);
        o.setCommentCount(23);
        o.setCollectCount(42);
        o.setCreateTime(LocalDateTime.of(2026, 6, 20, 9, 12, 0));
        o.setUpdateTime(LocalDateTime.of(2026, 6, 20, 9, 10, 0));

        InteractionDO reply = new InteractionDO();
        reply.setId(9L);
        reply.setContent("这个方案还有效吗？");
        reply.setUserName("李四");
        reply.setCreateTime(LocalDateTime.of(2026, 6, 25, 9, 20, 0));

        ProfilePublishVO v = convert.toPublishVO(o, Collections.singletonList(convert.toReplyVO(reply)));
        assertEquals("opportunity", v.getType());
        assertEquals(Boolean.FALSE, v.getIsAdopted());
        assertEquals(1, v.getReplies().size());

        String json = om.writeValueAsString(v);
        assertTrue(json.contains("\"opportunity_id\""), json);
        assertTrue(json.contains("\"view_count\""), json);
        assertTrue(json.contains("\"is_adopted\""), json);
        assertTrue(json.contains("\"edited_at\""), json);
        assertTrue(json.contains("\"from_name\""), json);   // 嵌套 reply 键
        assertFalse(json.contains("viewCount"), "不得出现 camelCase：" + json);
    }

    @Test
    void publishVO_requirement_adoption_derives_from_adopted_response_id() {
        OpportunityRequestDO r = new OpportunityRequestDO();
        r.setId(2001L);
        r.setTitle("需求标题");
        r.setAdoptedResponseId(8001L);   // 已选采纳方案
        r.setViewCount(156);
        r.setCreateTime(LocalDateTime.of(2026, 6, 10, 14, 20, 0));

        ProfilePublishVO v = convert.toPublishVO(r, Collections.emptyList());
        assertEquals("requirement", v.getType());
        assertEquals("published", v.getStatus());
        assertEquals(Boolean.TRUE, v.getIsAdopted());
    }

    @Test
    void solutionVO_derives_title_summary_from_content_and_best_flag() throws Exception {
        SolutionResponseDO s = new SolutionResponseDO();
        s.setId(8001L);
        s.setRequestId(2001L);
        s.setContent("<p>以 BC660K-GL 为核心，配套 DTU 与云平台对接，PSM 深度休眠下典型 5 年续航。</p>");
        s.setCreateTime(LocalDateTime.of(2026, 6, 10, 15, 30, 0));

        OpportunityRequestDO req = new OpportunityRequestDO();
        req.setId(2001L);
        req.setTitle("智慧水表 NB-IoT 选型需求");
        req.setPublisherName("赵敏");
        req.setPublisherDeptName("行业解决方案部");
        req.setAdoptedResponseId(8001L);   // 采纳的正是本方案
        req.setUpdateTime(LocalDateTime.of(2026, 6, 12, 9, 0, 0));

        ProfileSolutionVO v = convert.toSolutionVO(s, req);
        assertEquals(Long.valueOf(8001L), v.getSolutionId());
        assertEquals("智慧水表 NB-IoT 选型需求", v.getRequestTitle());
        assertEquals(Long.valueOf(2001L), v.getRequestId());
        assertEquals(Boolean.TRUE, v.getIsBest());
        assertEquals("赵敏", v.getAdopterName());
        // title/summary 由富文本去标签派生，不含标签
        assertFalse(v.getSummary().contains("<"), "summary 应已去标签：" + v.getSummary());
        assertTrue(v.getSummary().startsWith("以 BC660K-GL"), v.getSummary());

        String json = om.writeValueAsString(v);
        assertTrue(json.contains("\"request_title\""), json);
        assertTrue(json.contains("\"adopter_dept_name\""), json);
        assertTrue(json.contains("\"is_best\""), json);
        assertFalse(json.contains("requestTitle"), "不得出现 camelCase：" + json);
    }

    @Test
    void solutionVO_not_best_when_request_adopted_other() {
        SolutionResponseDO s = new SolutionResponseDO();
        s.setId(8002L);
        s.setRequestId(2002L);
        s.setContent("<p>另一个方案</p>");

        OpportunityRequestDO req = new OpportunityRequestDO();
        req.setId(2002L);
        req.setTitle("其它需求");
        req.setAdoptedResponseId(9999L);   // 采纳的是别人的方案

        ProfileSolutionVO v = convert.toSolutionVO(s, req);
        assertEquals(Boolean.FALSE, v.getIsBest());
        // 非最佳：采纳人信息不外泄
        org.junit.jupiter.api.Assertions.assertNull(v.getAdopterName());
        org.junit.jupiter.api.Assertions.assertNull(v.getAdoptedAt());
    }

    @Test
    void replyVO_maps_user_name_to_from_name() {
        InteractionDO i = new InteractionDO();
        i.setId(9L);
        i.setContent("已联系客户");
        i.setUserName("王五");
        i.setCreateTime(LocalDateTime.of(2026, 6, 26, 14, 30, 0));

        ProfilePublishReplyVO v = convert.toReplyVO(i);
        assertEquals(Long.valueOf(9L), v.getReplyId());
        assertEquals("王五", v.getFromName());
        assertEquals("已联系客户", v.getContent());
    }
}
