package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.quectel.web.cloud.salesleadhubserver.convert.DashboardConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ViewLogDao;
import com.quectel.web.cloud.salesleadhubserver.dto.DashboardQueryDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.DashboardService;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardCategoryDistVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardHotContentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardPageHeatVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardPieSegVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardTrendMapVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 运营数据看板只读聚合实现。入口 {@code requireAnyRole(ROLE_ADMIN)} fail-closed。
 *
 * <p>取数一次性 list 全表后在内存分桶/聚合（数据量小），不写复杂原生 SQL。
 * 环比与上一等长周期比较，prev=0 → mom=0（不造假）；所有除法防 0。
 * 页面热力无埋点表，用 view_log 的 target_type 分布近似（下期补精确埋点）。</p>
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    /** 饼图/趋势固定色板，按序轮询。 */
    private static final String[] PALETTE = {
            "#1677ff", "#52c41a", "#faad14", "#ff4d4f", "#13c2c2",
            "#722ed1", "#eb2f96", "#fa8c16", "#2f54eb", "#a0d911"
    };

    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_ADOPTED = "Adopted";

    private final OpportunityDao opportunityDao;
    private final RequirementDao requirementDao;
    private final ViewLogDao viewLogDao;
    private final OpportunityCategoryDao opportunityCategoryDao;
    private final RequestCategoryDao requestCategoryDao;
    private final CategoryDao categoryDao;
    private final CurrentUserResolver currentUser;
    private final DashboardConvert convert;

    public DashboardServiceImpl(OpportunityDao opportunityDao,
                                RequirementDao requirementDao,
                                ViewLogDao viewLogDao,
                                OpportunityCategoryDao opportunityCategoryDao,
                                RequestCategoryDao requestCategoryDao,
                                CategoryDao categoryDao,
                                CurrentUserResolver currentUser,
                                DashboardConvert convert) {
        this.opportunityDao = opportunityDao;
        this.requirementDao = requirementDao;
        this.viewLogDao = viewLogDao;
        this.opportunityCategoryDao = opportunityCategoryDao;
        this.requestCategoryDao = requestCategoryDao;
        this.categoryDao = categoryDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardVO dashboard(DashboardQueryDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        LocalDateTime now = LocalDateTime.now();
        int days = rangeToDays(dto == null ? null : dto.getRange());
        LocalDateTime curStart = now.minusDays(days);
        LocalDateTime prevStart = now.minusDays(2L * days);

        // 一次性取数
        List<ViewLogDO> views = viewLogDao.list();
        List<OpportunityDO> opps = opportunityDao.list();
        List<OpportunityRequestDO> reqs = requirementDao.list();
        List<OpportunityCategoryDO> oppCats = opportunityCategoryDao.list();
        List<RequestCategoryDO> reqCats = requestCategoryDao.list();
        Map<Long, String> catNames = categoryDao.list().stream()
                .collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName, (a, b) -> a));

        DashboardVO vo = new DashboardVO();

        // ---- UV / PV / 活跃用户（当前 vs 上一周期）----
        int uv = distinctUsers(views, curStart, now);
        int pv = pv(views, curStart, now);
        int prevUv = distinctUsers(views, prevStart, curStart);
        int prevPv = pv(views, prevStart, curStart);
        vo.setUv(uv);
        vo.setPv(pv);
        vo.setUvMom(mom(uv, prevUv));
        vo.setPvMom(mom(pv, prevPv));
        vo.setActiveUsers(uv);                 // 与 uv 同源同口径
        vo.setActiveUsersMom(mom(uv, prevUv));

        // ---- 周发布（商机 published + 需求，按 create_time 落周期）----
        int curPublish = publishCount(opps, reqs, curStart, now);
        int prevPublish = publishCount(opps, reqs, prevStart, curStart);
        vo.setWeekPublish(curPublish);
        vo.setWeekPublishMom(mom(curPublish, prevPublish));

        // ---- 响应率 / 采纳率 ----
        double curResp = responseRate(reqs, curStart, now);
        double prevResp = responseRate(reqs, prevStart, curStart);
        vo.setResponseRate(curResp);
        vo.setResponseRateMom(momDouble(curResp, prevResp));
        double curAdopt = adoptRate(reqs, curStart, now);
        double prevAdopt = adoptRate(reqs, prevStart, curStart);
        vo.setAdoptRate(curAdopt);
        vo.setAdoptRateMom(momDouble(curAdopt, prevAdopt));

        // ---- 热门内容（周期内 view_count 前 5 商机）----
        vo.setHotContents(hotContents(opps, curStart, now));

        // ---- 需求分类分布 ----
        vo.setCategoryDist(categoryDist(reqCats, catNames));

        // ---- 页面热力（view_log target_type 近似）----
        vo.setPageHeat(pageHeat(views, curStart, now));

        // ---- 趋势（四粒度各一条序列）----
        vo.setWeekPublishTrend(publishTrend(opps, reqs, now));
        vo.setResponseRateTrend(responseTrend(reqs, now));

        // ---- 饼图（商机/需求分类分布）----
        vo.setOppCategoryPie(categoryPie(oppCats.stream().map(OpportunityCategoryDO::getCategoryId), catNames));
        vo.setDemandCategoryPie(categoryPie(reqCats.stream().map(RequestCategoryDO::getCategoryId), catNames));

        // ---- 近 24h 时段活跃 ----
        vo.setHourlyActive(hourlyActive(views, now));

        vo.setUpdatedAt(now);
        return vo;
    }

    // ---------- 周期映射 ----------

    private int rangeToDays(String range) {
        if ("last4w".equals(range)) {
            return 28;
        }
        if ("last12w".equals(range)) {
            return 84;
        }
        if ("last6m".equals(range)) {
            return 180;
        }
        return 7;   // last7d 默认
    }

    // ---------- UV/PV ----------

    private int distinctUsers(List<ViewLogDO> views, LocalDateTime start, LocalDateTime end) {
        return (int) views.stream()
                .filter(v -> inRange(v.getViewedAt(), start, end))
                .map(ViewLogDO::getUserId).filter(Objects::nonNull).distinct().count();
    }

    private int pv(List<ViewLogDO> views, LocalDateTime start, LocalDateTime end) {
        return (int) views.stream().filter(v -> inRange(v.getViewedAt(), start, end)).count();
    }

    // ---------- 发布 / 响应 / 采纳 ----------

    private int publishCount(List<OpportunityDO> opps, List<OpportunityRequestDO> reqs,
                             LocalDateTime start, LocalDateTime end) {
        long o = opps.stream()
                .filter(d -> STATUS_PUBLISHED.equals(d.getStatus()) && inRange(d.getCreateTime(), start, end))
                .count();
        long r = reqs.stream().filter(d -> inRange(d.getCreateTime(), start, end)).count();
        return (int) (o + r);
    }

    private double responseRate(List<OpportunityRequestDO> reqs, LocalDateTime start, LocalDateTime end) {
        List<OpportunityRequestDO> in = reqs.stream()
                .filter(d -> inRange(d.getCreateTime(), start, end)).collect(Collectors.toList());
        long responded = in.stream().filter(d -> d.getResponseCount() != null && d.getResponseCount() > 0).count();
        return pct(responded, in.size());
    }

    private double adoptRate(List<OpportunityRequestDO> reqs, LocalDateTime start, LocalDateTime end) {
        List<OpportunityRequestDO> in = reqs.stream()
                .filter(d -> inRange(d.getCreateTime(), start, end)).collect(Collectors.toList());
        long adopted = in.stream().filter(d -> STATUS_ADOPTED.equals(d.getStatus())).count();
        return pct(adopted, in.size());
    }

    // ---------- 热门内容 ----------

    private List<DashboardHotContentVO> hotContents(List<OpportunityDO> opps,
                                                    LocalDateTime start, LocalDateTime end) {
        return opps.stream()
                .filter(d -> STATUS_PUBLISHED.equals(d.getStatus()) && inRange(d.getCreateTime(), start, end))
                .sorted(Comparator.comparingInt((OpportunityDO d) -> d.getViewCount() == null ? 0 : d.getViewCount()).reversed())
                .limit(5)
                .map(convert::toHotContentVO)
                .collect(Collectors.toList());
    }

    // ---------- 分类分布 ----------

    private List<DashboardCategoryDistVO> categoryDist(List<RequestCategoryDO> reqCats, Map<Long, String> catNames) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (RequestCategoryDO rc : reqCats) {
            counts.merge(rc.getCategoryId(), 1, Integer::sum);
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        return counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(e -> convert.toCategoryDistVO(
                        catNames.getOrDefault(e.getKey(), "未知分类"), e.getValue(), pct(e.getValue(), total)))
                .collect(Collectors.toList());
    }

    // ---------- 页面热力（近似）----------

    private List<DashboardPageHeatVO> pageHeat(List<ViewLogDO> views, LocalDateTime start, LocalDateTime end) {
        List<ViewLogDO> in = views.stream()
                .filter(v -> inRange(v.getViewedAt(), start, end)).collect(Collectors.toList());
        int total = in.size();
        long opp = in.stream().filter(v -> "Opportunity".equals(v.getTargetType())).count();
        long req = in.stream().filter(v -> "Request".equals(v.getTargetType())).count();
        long other = total - opp - req;
        List<DashboardPageHeatVO> out = new ArrayList<>();
        out.add(convert.toPageHeatVO("商机详情", (int) opp, pct(opp, total)));
        out.add(convert.toPageHeatVO("需求详情", (int) req, pct(req, total)));
        out.add(convert.toPageHeatVO("其他", (int) other, pct(other, total)));
        return out;
    }

    // ---------- 趋势 ----------

    private DashboardTrendMapVO publishTrend(List<OpportunityDO> opps, List<OpportunityRequestDO> reqs, LocalDateTime now) {
        DashboardTrendMapVO m = new DashboardTrendMapVO();
        m.setLast7d(mapBuckets(dailyBuckets(now, 7), b -> (Number) publishCount(opps, reqs, b[0], b[1])));
        m.setLast4w(mapBuckets(rollingBuckets(now, 4, 7), b -> (Number) publishCount(opps, reqs, b[0], b[1])));
        m.setLast12w(mapBuckets(rollingBuckets(now, 12, 7), b -> (Number) publishCount(opps, reqs, b[0], b[1])));
        m.setLast6m(mapBuckets(monthlyBuckets(now, 6), b -> (Number) publishCount(opps, reqs, b[0], b[1])));
        return m;
    }

    private DashboardTrendMapVO responseTrend(List<OpportunityRequestDO> reqs, LocalDateTime now) {
        DashboardTrendMapVO m = new DashboardTrendMapVO();
        m.setLast7d(mapBuckets(dailyBuckets(now, 7), b -> (Number) responseRate(reqs, b[0], b[1])));
        m.setLast4w(mapBuckets(rollingBuckets(now, 4, 7), b -> (Number) responseRate(reqs, b[0], b[1])));
        m.setLast12w(mapBuckets(rollingBuckets(now, 12, 7), b -> (Number) responseRate(reqs, b[0], b[1])));
        m.setLast6m(mapBuckets(monthlyBuckets(now, 6), b -> (Number) responseRate(reqs, b[0], b[1])));
        return m;
    }

    private List<Number> mapBuckets(List<LocalDateTime[]> buckets, java.util.function.Function<LocalDateTime[], Number> fn) {
        return buckets.stream().map(fn).collect(Collectors.toList());
    }

    /** 近 count 个自然日的 [start,end) 桶（最早在前）。 */
    private List<LocalDateTime[]> dailyBuckets(LocalDateTime now, int count) {
        List<LocalDateTime[]> out = new ArrayList<>();
        for (int i = count - 1; i >= 0; i--) {
            LocalDateTime start = now.toLocalDate().minusDays(i).atStartOfDay();
            out.add(new LocalDateTime[]{start, start.plusDays(1)});
        }
        return out;
    }

    /** 近 count 个滚动窗口（每个 daysPerUnit 天）的 [start,end) 桶（最早在前）。 */
    private List<LocalDateTime[]> rollingBuckets(LocalDateTime now, int count, int daysPerUnit) {
        List<LocalDateTime[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalDateTime start = now.minusDays((long) (count - i) * daysPerUnit);
            LocalDateTime end = now.minusDays((long) (count - 1 - i) * daysPerUnit);
            out.add(new LocalDateTime[]{start, end});
        }
        return out;
    }

    /** 近 count 个自然月滚动窗口的 [start,end) 桶（最早在前）。 */
    private List<LocalDateTime[]> monthlyBuckets(LocalDateTime now, int count) {
        List<LocalDateTime[]> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalDateTime start = now.minusMonths(count - i);
            LocalDateTime end = now.minusMonths(count - 1 - i);
            out.add(new LocalDateTime[]{start, end});
        }
        return out;
    }

    // ---------- 饼图 ----------

    private List<DashboardPieSegVO> categoryPie(java.util.stream.Stream<Long> categoryIdStream, Map<Long, String> catNames) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        categoryIdStream.forEach(id -> counts.merge(id, 1, Integer::sum));
        List<Map.Entry<Long, Integer>> sorted = counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
        List<DashboardPieSegVO> out = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<Long, Integer> e : sorted) {
            out.add(convert.toPieSegVO(catNames.getOrDefault(e.getKey(), "未知分类"),
                    e.getValue(), PALETTE[idx % PALETTE.length]));
            idx++;
        }
        return out;
    }

    // ---------- 时段活跃 ----------

    private List<Integer> hourlyActive(List<ViewLogDO> views, LocalDateTime now) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            LocalDateTime start = now.minusHours(24L - i);
            LocalDateTime end = now.minusHours(23L - i);
            out.add((int) views.stream().filter(v -> inRange(v.getViewedAt(), start, end)).count());
        }
        return out;
    }

    // ---------- 通用 ----------

    private boolean inRange(LocalDateTime t, LocalDateTime start, LocalDateTime end) {
        return t != null && !t.isBefore(start) && t.isBefore(end);
    }

    /** 整数环比：prev=0 → 0（不造假）。 */
    private double mom(int cur, int prev) {
        return momDouble(cur, prev);
    }

    /** 比率环比：prev=0 → 0。 */
    private double momDouble(double cur, double prev) {
        if (prev == 0d) {
            return 0d;
        }
        return round1((cur - prev) / prev * 100.0);
    }

    /** 占比（%）：total=0 → 0，保留 1 位小数。 */
    private double pct(long part, long total) {
        if (total == 0L) {
            return 0d;
        }
        return round1(part * 100.0 / total);
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
