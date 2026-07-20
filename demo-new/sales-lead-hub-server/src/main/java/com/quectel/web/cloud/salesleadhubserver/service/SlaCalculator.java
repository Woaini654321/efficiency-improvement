package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * SLA 首响时效派生器（纯函数，无任何依赖）。
 *
 * <p><b>口径（PRD 决策基线）</b>：只监控<b>首响时限</b>，计时起点 = {@code create_time}；
 * 阈值 critical 2h / urgent 4h / normal 24h。收到首个方案（{@code response_count>0} 或
 * {@code status != 'Pending'}）即停表 → {@code responded}。未停表按服务器<b>当前时间</b>实时算：
 * 超阈值 → {@code overdue}；达阈值 80% → {@code warning}；否则 {@code normal}。</p>
 *
 * <p><b>升级</b>：超时后每再过一个「该等级首响时限」升一级，L0→L1→L2→L3 封顶
 * （L1 在到达 deadline 即 1×阈值时触发，L2 在 2×，L3 在 3× 及以后）。</p>
 *
 * <p>所有方法接受 {@code now} 参数（而非内部读时钟），派生逻辑因此可离线单测——
 * 这是「派生逻辑抽成可注入时钟」的落地方式。库里的 sla_status/escalation_level 存量值
 * 一律不参与计算，仅以实时结果为准。</p>
 */
@Component
public class SlaCalculator {

    public static final String STATUS_NORMAL = "normal";
    public static final String STATUS_WARNING = "warning";
    public static final String STATUS_OVERDUE = "overdue";
    public static final String STATUS_RESPONDED = "responded";

    private static final int CRITICAL_MINUTES = 2 * 60;
    private static final int URGENT_MINUTES = 4 * 60;
    private static final int NORMAL_MINUTES = 24 * 60;

    /** 首响阈值（分钟）。未知 urgency 一律按 normal 兜底，宁松勿误报超时。 */
    public int thresholdMinutes(String urgency) {
        if ("critical".equals(urgency)) {
            return CRITICAL_MINUTES;
        }
        if ("urgent".equals(urgency)) {
            return URGENT_MINUTES;
        }
        return NORMAL_MINUTES;
    }

    /** 是否已停表：收到首个方案（response_count>0）或状态已离开 Pending。 */
    public boolean isResponded(OpportunityRequestDO d) {
        boolean hasResponse = d.getResponseCount() != null && d.getResponseCount() > 0;
        boolean leftPending = d.getStatus() != null && !"Pending".equals(d.getStatus());
        return hasResponse || leftPending;
    }

    /** deadline = create_time + 阈值；create_time 缺失返回 null。 */
    public LocalDateTime deadline(OpportunityRequestDO d) {
        if (d.getCreateTime() == null) {
            return null;
        }
        return d.getCreateTime().plusMinutes(thresholdMinutes(d.getUrgency()));
    }

    /** 已计时分钟数（负值夹到 0）；create_time 缺失按 0。 */
    private long elapsedMinutes(OpportunityRequestDO d, LocalDateTime now) {
        if (d.getCreateTime() == null) {
            return 0L;
        }
        long m = Duration.between(d.getCreateTime(), now).toMinutes();
        return Math.max(0L, m);
    }

    public String deriveStatus(OpportunityRequestDO d, LocalDateTime now) {
        if (isResponded(d)) {
            return STATUS_RESPONDED;
        }
        int t = thresholdMinutes(d.getUrgency());
        long elapsed = elapsedMinutes(d, now);
        if (elapsed >= t) {
            return STATUS_OVERDUE;
        }
        if (elapsed >= (long) Math.ceil(t * 0.8)) {
            return STATUS_WARNING;
        }
        return STATUS_NORMAL;
    }

    public String deriveEscalationLevel(OpportunityRequestDO d, LocalDateTime now) {
        if (isResponded(d)) {
            return "L0";
        }
        int t = thresholdMinutes(d.getUrgency());
        long elapsed = elapsedMinutes(d, now);
        if (elapsed < t) {
            return "L0";
        }
        int level = (int) Math.min(3L, elapsed / t);  // 1×→L1，2×→L2，3× 及以上→L3
        return "L" + level;
    }

    /** 已达升级级数（0~3），供时间线生成逐级行。 */
    public int escalationLevelInt(OpportunityRequestDO d, LocalDateTime now) {
        String lvl = deriveEscalationLevel(d, now);
        return lvl.charAt(1) - '0';
    }

    public String deriveRemainingText(OpportunityRequestDO d, LocalDateTime now) {
        if (isResponded(d)) {
            return "已响应";
        }
        LocalDateTime deadline = deadline(d);
        if (deadline == null) {
            return "";
        }
        long diff = Duration.between(now, deadline).toMinutes();  // >0 剩余，<0 超时
        if (diff >= 0) {
            return "剩余 " + formatDuration(diff);
        }
        return "已超时 " + formatDuration(-diff);
    }

    /** 分钟 → 「X时Y分」（对齐 mock 展示，不做天进位）。 */
    public String formatDuration(long minutes) {
        long m = Math.max(0L, minutes);
        return (m / 60) + "时" + (m % 60) + "分";
    }
}
