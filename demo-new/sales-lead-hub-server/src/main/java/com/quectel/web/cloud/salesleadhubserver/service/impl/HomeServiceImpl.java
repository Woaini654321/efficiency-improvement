package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.web.cloud.salesleadhubserver.convert.HomeConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.AnnouncementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionPostDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SubscriptionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ViewLogDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SubscriptionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.HomeService;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeAnnouncementVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeDashboardVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomePostVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeSolutionVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeStatsVO;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页工作台聚合实现。登录即可（不校业务角色）；个性化段（我的待办 / 是否订阅）在取不到
 * 当前用户时安全降级为空，不阻断整页。
 *
 * <p>取数一律走 {@link com.baomidou.mybatisplus.extension.service.IService} 的
 * {@code count/list/page(wrapper)} 方法（而非 {@code lambdaQuery()} 链式），这样 service 单测
 * 能直接 mock 出返回值、无需 mock MP 链式包装器。数据量小、无复杂 JOIN。</p>
 */
@Service
public class HomeServiceImpl implements HomeService {

    private static final int HOT_TAGS_LIMIT = 12;
    private static final int QUICK_TASKS_LIMIT = 5;
    private static final int HOT_SOLUTIONS_LIMIT = 5;
    private static final int ANNOUNCEMENTS_LIMIT = 3;
    private static final int HOT_POSTS_LIMIT = 8;

    private final OpportunityDao opportunityDao;
    private final RequirementDao requirementDao;
    private final DiscussionPostDao discussionPostDao;
    private final MeetingTaskDao meetingTaskDao;
    private final AnnouncementDao announcementDao;
    private final ViewLogDao viewLogDao;
    private final SubscriptionDao subscriptionDao;
    private final OpportunityCategoryDao opportunityCategoryDao;
    private final RequestCategoryDao requestCategoryDao;
    private final CategoryDao categoryDao;
    private final CurrentUserResolver currentUser;
    private final HomeConvert convert;

    public HomeServiceImpl(OpportunityDao opportunityDao,
                           RequirementDao requirementDao,
                           DiscussionPostDao discussionPostDao,
                           MeetingTaskDao meetingTaskDao,
                           AnnouncementDao announcementDao,
                           ViewLogDao viewLogDao,
                           SubscriptionDao subscriptionDao,
                           OpportunityCategoryDao opportunityCategoryDao,
                           RequestCategoryDao requestCategoryDao,
                           CategoryDao categoryDao,
                           CurrentUserResolver currentUser,
                           HomeConvert convert) {
        this.opportunityDao = opportunityDao;
        this.requirementDao = requirementDao;
        this.discussionPostDao = discussionPostDao;
        this.meetingTaskDao = meetingTaskDao;
        this.announcementDao = announcementDao;
        this.viewLogDao = viewLogDao;
        this.subscriptionDao = subscriptionDao;
        this.opportunityCategoryDao = opportunityCategoryDao;
        this.requestCategoryDao = requestCategoryDao;
        this.categoryDao = categoryDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeDashboardVO dashboard() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);
        SysUserDO me = currentUser.currentOrNull();

        HomeDashboardVO vo = new HomeDashboardVO();
        vo.setStats(buildStats(weekAgo));
        vo.setHotTags(buildHotTags());
        vo.setQuickTasks(buildQuickTasks(me, now));
        vo.setHotSolutions(buildHotSolutions(me));
        vo.setAnnouncements(buildAnnouncements());
        vo.setHotPosts(buildHotPosts());
        return vo;
    }

    // ---------- private ----------

    private HomeStatsVO buildStats(LocalDateTime weekAgo) {
        int solutionTotal = (int) opportunityDao.count(new LambdaQueryWrapper<OpportunityDO>()
                .eq(OpportunityDO::getStatus, "published"));
        int pending = (int) requirementDao.count(new LambdaQueryWrapper<OpportunityRequestDO>()
                .eq(OpportunityRequestDO::getStatus, "Pending"));
        int weekDisc = (int) discussionPostDao.count(new LambdaQueryWrapper<DiscussionPostDO>()
                .ge(DiscussionPostDO::getCreateTime, weekAgo));
        long activeUsers = viewLogDao.list(new LambdaQueryWrapper<ViewLogDO>()
                .ge(ViewLogDO::getViewedAt, weekAgo)).stream()
                .map(ViewLogDO::getUserId).filter(Objects::nonNull).distinct().count();
        return convert.toStats(solutionTotal, pending, weekDisc, (int) activeUsers);
    }

    private List<String> buildHotTags() {
        Map<Long, String> activeNames = categoryDao.list(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getIsActive, 1)).stream()
                .collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName, (a, b) -> a));
        if (activeNames.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (OpportunityCategoryDO oc : opportunityCategoryDao.list()) {
            counts.merge(oc.getCategoryId(), 1, Integer::sum);
        }
        for (RequestCategoryDO rc : requestCategoryDao.list()) {
            counts.merge(rc.getCategoryId(), 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .filter(e -> activeNames.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(HOT_TAGS_LIMIT)
                .map(e -> activeNames.get(e.getKey()))
                .collect(Collectors.toList());
    }

    private List<HomeTaskVO> buildQuickTasks(SysUserDO me, LocalDateTime now) {
        if (me == null) {
            return Collections.emptyList();
        }
        Long uid = me.getId();
        String myName = me.getName();
        List<MeetingTaskDO> candidates = meetingTaskDao.list(new LambdaQueryWrapper<MeetingTaskDO>()
                .in(MeetingTaskDO::getStatus, Arrays.asList("pending", "processing")));
        List<MeetingTaskDO> mine = candidates.stream()
                .filter(t -> assigneeMatches(t, uid, myName))
                .sorted(Comparator.comparing(MeetingTaskDO::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(QUICK_TASKS_LIMIT)
                .collect(Collectors.toList());
        List<HomeTaskVO> out = new ArrayList<>();
        for (MeetingTaskDO t : mine) {
            boolean overdue = t.getDeadline() != null && t.getDeadline().isBefore(now);
            out.add(convert.toTaskVO(t, overdue));
        }
        return out;
    }

    /** 执行人匹配：assignee_ids 含当前用户 id（主键），或 assignee_names 含姓名（过渡态兜底）。 */
    private boolean assigneeMatches(MeetingTaskDO t, Long uid, String myName) {
        if (t.getAssigneeIds() != null && t.getAssigneeIds().contains(uid)) {
            return true;
        }
        return myName != null && t.getAssigneeNames() != null && t.getAssigneeNames().contains(myName);
    }

    private List<HomeSolutionVO> buildHotSolutions(SysUserDO me) {
        List<OpportunityDO> top = opportunityDao.page(new Page<>(1, HOT_SOLUTIONS_LIMIT),
                new LambdaQueryWrapper<OpportunityDO>()
                        .eq(OpportunityDO::getStatus, "published")
                        .orderByDesc(OpportunityDO::getViewCount)).getRecords();
        if (top.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> subCatIds = me == null ? Collections.<Long>emptySet()
                : subscriptionDao.list(new LambdaQueryWrapper<SubscriptionDO>()
                        .eq(SubscriptionDO::getUserId, me.getId())).stream()
                .map(SubscriptionDO::getCategoryId).collect(Collectors.toSet());

        // 这批商机的分类关联一次性查出，避免逐行 N+1
        Map<Long, Set<Long>> catsByOpp = new HashMap<>();
        if (!subCatIds.isEmpty()) {
            List<Long> oppIds = top.stream().map(OpportunityDO::getId).collect(Collectors.toList());
            for (OpportunityCategoryDO oc : opportunityCategoryDao.list(new LambdaQueryWrapper<OpportunityCategoryDO>()
                    .in(OpportunityCategoryDO::getOpportunityId, oppIds))) {
                catsByOpp.computeIfAbsent(oc.getOpportunityId(), k -> new HashSet<>()).add(oc.getCategoryId());
            }
        }

        List<HomeSolutionVO> out = new ArrayList<>();
        int rank = 1;
        for (OpportunityDO d : top) {
            boolean subscribed = !subCatIds.isEmpty()
                    && catsByOpp.getOrDefault(d.getId(), Collections.emptySet()).stream().anyMatch(subCatIds::contains);
            out.add(convert.toSolutionVO(d, rank++, subscribed));
        }
        return out;
    }

    private List<HomeAnnouncementVO> buildAnnouncements() {
        return announcementDao.page(new Page<>(1, ANNOUNCEMENTS_LIMIT),
                        new LambdaQueryWrapper<AnnouncementDO>()
                                .eq(AnnouncementDO::getStatus, "published")
                                .orderByDesc(AnnouncementDO::getIsPinned)
                                .orderByDesc(AnnouncementDO::getPublishedAt)).getRecords().stream()
                .map(convert::toAnnouncementVO)
                .collect(Collectors.toList());
    }

    private List<HomePostVO> buildHotPosts() {
        // 讨论帖数据量小，全量取出后按 reply_count 加权 + view_count 排序取前 N
        return discussionPostDao.list().stream()
                .sorted(Comparator.comparingInt(this::postScore).reversed())
                .limit(HOT_POSTS_LIMIT)
                .map(convert::toPostVO)
                .collect(Collectors.toList());
    }

    /** 热度权重：回帖比浏览更能代表讨论热度，回帖 ×3 + 浏览。 */
    private int postScore(DiscussionPostDO d) {
        int reply = d.getReplyCount() == null ? 0 : d.getReplyCount();
        int view = d.getViewCount() == null ? 0 : d.getViewCount();
        return reply * 3 + view;
    }
}
