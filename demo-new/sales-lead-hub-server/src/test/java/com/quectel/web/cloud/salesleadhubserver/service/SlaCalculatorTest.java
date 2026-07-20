package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SLA 派生器纯函数单测：四种 sla_status、升级阶梯、剩余/超时文案。
 *
 * <p>全部用固定 {@code now} 参数驱动，离线可跑、与服务器真实时钟无关——这是「派生逻辑
 * 抽成可注入时钟」的验证点。</p>
 */
class SlaCalculatorTest {

    private final SlaCalculator calc = new SlaCalculator();

    private OpportunityRequestDO req(String urgency, LocalDateTime createTime, int responseCount, String status) {
        OpportunityRequestDO d = new OpportunityRequestDO();
        d.setUrgency(urgency);
        d.setCreateTime(createTime);
        d.setResponseCount(responseCount);
        d.setStatus(status);
        return d;
    }

    // ---------- 阈值 ----------

    @Test
    void thresholds_are_2h_4h_24h() {
        assertEquals(120, calc.thresholdMinutes("critical"));
        assertEquals(240, calc.thresholdMinutes("urgent"));
        assertEquals(1440, calc.thresholdMinutes("normal"));
        assertEquals(1440, calc.thresholdMinutes("unknown"), "未知等级按 normal 兜底");
    }

    // ---------- 四种状态 ----------

    @Test
    void responded_when_has_response_or_left_pending() {
        LocalDateTime base = LocalDateTime.of(2027, 1, 15, 7, 0);
        LocalDateTime now = base.plusHours(100); // 早已超阈值，但已响应应停表
        OpportunityRequestDO withResp = req("critical", base, 1, "Pending");
        assertEquals(SlaCalculator.STATUS_RESPONDED, calc.deriveStatus(withResp, now));
        assertEquals("L0", calc.deriveEscalationLevel(withResp, now));
        assertEquals("已响应", calc.deriveRemainingText(withResp, now));

        OpportunityRequestDO adopted = req("critical", base, 0, "Adopted");
        assertEquals(SlaCalculator.STATUS_RESPONDED, calc.deriveStatus(adopted, now));
    }

    @Test
    void normal_when_well_within_window() {
        LocalDateTime base = LocalDateTime.of(2027, 1, 15, 7, 0);
        OpportunityRequestDO d = req("critical", base, 0, "Pending"); // T=120min
        LocalDateTime now = base.plusMinutes(30);                    // 30 < 96
        assertEquals(SlaCalculator.STATUS_NORMAL, calc.deriveStatus(d, now));
        assertEquals("剩余 1时30分", calc.deriveRemainingText(d, now));
        assertEquals("L0", calc.deriveEscalationLevel(d, now));
    }

    @Test
    void warning_at_80_percent_of_window() {
        LocalDateTime base = LocalDateTime.of(2027, 1, 15, 7, 0);
        OpportunityRequestDO d = req("critical", base, 0, "Pending"); // T=120min，80%=96min
        LocalDateTime now = base.plusMinutes(100);                   // 96<=100<120
        assertEquals(SlaCalculator.STATUS_WARNING, calc.deriveStatus(d, now));
        assertEquals("剩余 0时20分", calc.deriveRemainingText(d, now));
    }

    @Test
    void overdue_past_window() {
        LocalDateTime base = LocalDateTime.of(2027, 1, 15, 7, 0);
        OpportunityRequestDO d = req("critical", base, 0, "Pending"); // T=120min
        LocalDateTime now = base.plusMinutes(150);                   // 超时 30min
        assertEquals(SlaCalculator.STATUS_OVERDUE, calc.deriveStatus(d, now));
        assertEquals("已超时 0时30分", calc.deriveRemainingText(d, now));
    }

    // ---------- 升级阶梯 ----------

    @Test
    void escalation_ladder_L1_L2_L3_capped() {
        LocalDateTime base = LocalDateTime.of(2027, 1, 15, 7, 0);
        OpportunityRequestDO d = req("critical", base, 0, "Pending"); // T=120min

        assertEquals("L0", calc.deriveEscalationLevel(d, base.plusMinutes(119)));
        assertEquals("L1", calc.deriveEscalationLevel(d, base.plusMinutes(120)), "到达 1×阈值即 L1");
        assertEquals("L2", calc.deriveEscalationLevel(d, base.plusMinutes(240)), "2×阈值 L2");
        assertEquals("L3", calc.deriveEscalationLevel(d, base.plusMinutes(360)), "3×阈值 L3");
        assertEquals("L3", calc.deriveEscalationLevel(d, base.plusMinutes(2000)), "3× 以后封顶 L3");
    }

    // ---------- deadline ----------

    @Test
    void deadline_is_create_time_plus_threshold() {
        LocalDateTime base = LocalDateTime.of(2027, 1, 15, 7, 0);
        OpportunityRequestDO d = req("normal", base, 0, "Pending"); // T=24h
        assertEquals(base.plusHours(24), calc.deadline(d));
    }

    @Test
    void overdue_text_can_exceed_24h() {
        LocalDateTime base = LocalDateTime.of(2027, 1, 13, 12, 0);
        OpportunityRequestDO d = req("normal", base, 0, "Pending"); // T=24h，deadline 01-14 12:00
        LocalDateTime now = base.plusHours(48);                    // 超时 24h
        assertTrue(calc.deriveRemainingText(d, now).startsWith("已超时"));
        assertEquals("已超时 24时0分", calc.deriveRemainingText(d, now));
    }
}
