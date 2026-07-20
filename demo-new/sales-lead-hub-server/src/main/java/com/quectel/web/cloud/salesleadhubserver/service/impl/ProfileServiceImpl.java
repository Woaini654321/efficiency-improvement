package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.ProfileConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.InteractionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SubscriptionDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.ViewLogMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SolutionResponseDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SubscriptionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.ProfileService;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCenterVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCollectVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileHistoryVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfilePublishReplyVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfilePublishVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileSolutionVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SubscribedKeysVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SubscriptionNodeVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SubscriptionTreeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人中心聚合实现。
 *
 * <p>互动/方案计数与列表复用互动模块代理已建的 InteractionDao / SolutionResponseDao；
 * view_log 无 DAO，直接用 ViewLogMapper；商机/需求走各自 DAO。</p>
 *
 * <p>五段列表统一纪律：每段 SQL 层 LIMIT 100、按时间倒序；对目标标题采用「批量回查 + Map 命中」，
 * 命中不到（逻辑删被 @TableLogic 过滤 / 物理删）的行直接丢弃，保证前端拿到的都是可点开的存活项。</p>
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private static final String TYPE_COLLECT = "collect";
    private static final String TYPE_COMMENT = "comment";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_DRAFT = "draft";
    private static final int NOT_DELETED = 0;
    private static final int ACTIVE = 1;
    private static final int LIST_LIMIT = 100;

    /** interaction/view_log 里目标类型的存储值（首字母大写）。 */
    private static final String TARGET_OPPORTUNITY = "Opportunity";
    private static final String TARGET_REQUEST = "Request";
    /** 前端 adapter 侧的类型字面量（小写）。 */
    private static final String ADAPTER_OPPORTUNITY = "opportunity";
    private static final String ADAPTER_REQUIREMENT = "requirement";

    private final CurrentUserResolver currentUser;
    private final OpportunityDao opportunityDao;
    private final RequirementDao requirementDao;
    private final SubscriptionDao subscriptionDao;
    private final CategoryDao categoryDao;
    private final InteractionDao interactionDao;
    private final SolutionResponseDao solutionResponseDao;
    private final ViewLogMapper viewLogMapper;
    private final ProfileConvert convert;

    public ProfileServiceImpl(CurrentUserResolver currentUser,
                              OpportunityDao opportunityDao,
                              RequirementDao requirementDao,
                              SubscriptionDao subscriptionDao,
                              CategoryDao categoryDao,
                              InteractionDao interactionDao,
                              SolutionResponseDao solutionResponseDao,
                              ViewLogMapper viewLogMapper,
                              ProfileConvert convert) {
        this.currentUser = currentUser;
        this.opportunityDao = opportunityDao;
        this.requirementDao = requirementDao;
        this.subscriptionDao = subscriptionDao;
        this.categoryDao = categoryDao;
        this.interactionDao = interactionDao;
        this.solutionResponseDao = solutionResponseDao;
        this.viewLogMapper = viewLogMapper;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileCenterVO center() {
        // 只取当前登录人自己的档案：无本地档案即未开通，fail-closed 拒绝（不接受 userId 入参）
        SysUserDO me = currentUser.currentOrNull();
        if (me == null) {
            throw new BaseException(ErrorCode.FORBIDDEN, "当前账号尚未在本平台开通，请联系运营维护用户档案");
        }
        Long uid = me.getId();

        ProfileCenterVO vo = new ProfileCenterVO();
        vo.setUser(convert.toUserVO(me));
        vo.setStats(convert.toStatsVO(
                collectCount(uid),
                commentCount(uid),
                publishCount(uid),
                solutionCount(uid),
                draftCount(uid),
                viewCount(uid)));
        vo.setSubscriptionTree(buildTree());
        vo.setSubscribedKeys(buildSubscribedKeys(uid));
        vo.setCollects(buildCollects(uid));
        vo.setPublishes(buildPublishes(uid));
        vo.setSolutions(buildSolutions(uid));
        vo.setComments(buildComments(uid));
        vo.setViewHistory(buildViewHistory(uid));
        return vo;
    }

    // ---------- 统计计数（均限定当前登录人） ----------

    private long collectCount(Long uid) {
        return interactionDao.lambdaQuery()
                .eq(InteractionDO::getUserId, uid)
                .eq(InteractionDO::getType, TYPE_COLLECT)
                .count();
    }

    private long commentCount(Long uid) {
        return interactionDao.lambdaQuery()
                .eq(InteractionDO::getUserId, uid)
                .eq(InteractionDO::getType, TYPE_COMMENT)
                .eq(InteractionDO::getContentDeleted, NOT_DELETED)
                .count();
    }

    /** 我发布的：已发布商机 + 我发布的需求（需求无草稿态，全部计入）。 */
    private long publishCount(Long uid) {
        long opp = opportunityDao.lambdaQuery()
                .eq(OpportunityDO::getPublisherId, uid)
                .eq(OpportunityDO::getStatus, STATUS_PUBLISHED)
                .count();
        long req = requirementDao.lambdaQuery()
                .eq(OpportunityRequestDO::getPublisherId, uid)
                .count();
        return opp + req;
    }

    private long solutionCount(Long uid) {
        return solutionResponseDao.lambdaQuery()
                .eq(SolutionResponseDO::getResponderId, uid)
                .count();
    }

    private long draftCount(Long uid) {
        return opportunityDao.lambdaQuery()
                .eq(OpportunityDO::getPublisherId, uid)
                .eq(OpportunityDO::getStatus, STATUS_DRAFT)
                .count();
    }

    private long viewCount(Long uid) {
        return viewLogMapper.selectCount(new LambdaQueryWrapper<ViewLogDO>()
                .eq(ViewLogDO::getUserId, uid));
    }

    // ---------- 订阅树与已选 keys ----------

    /** 全部启用分类组成的森林，暂归 opportunity；requirement 空（category 无双域判别列，见 VO 说明）。 */
    private SubscriptionTreeVO buildTree() {
        List<CategoryDO> categories = categoryDao.lambdaQuery()
                .eq(CategoryDO::getIsActive, ACTIVE)
                .list();
        List<SubscriptionNodeVO> forest = convert.buildForest(categories);
        SubscriptionTreeVO tree = new SubscriptionTreeVO();
        tree.setOpportunity(forest);
        tree.setRequirement(new ArrayList<>());
        return tree;
    }

    /** 当前登录人已订阅分类 id（字符串），暂归 opportunity；requirement 空。 */
    private SubscribedKeysVO buildSubscribedKeys(Long uid) {
        List<String> keys = subscriptionDao.lambdaQuery()
                .eq(SubscriptionDO::getUserId, uid)
                .list().stream()
                .map(s -> String.valueOf(s.getCategoryId()))
                .collect(Collectors.toList());
        SubscribedKeysVO vo = new SubscribedKeysVO();
        vo.setOpportunity(keys);
        vo.setRequirement(Collections.emptyList());
        return vo;
    }

    // ---------- 五段列表 ----------

    /** 我的收藏：interaction(type=collect) 回查目标标题，逝去目标丢弃。 */
    private List<ProfileCollectVO> buildCollects(Long uid) {
        List<InteractionDO> rows = interactionDao.lambdaQuery()
                .eq(InteractionDO::getUserId, uid)
                .eq(InteractionDO::getType, TYPE_COLLECT)
                .orderByDesc(InteractionDO::getCreateTime)
                .last("LIMIT " + LIST_LIMIT)
                .list();
        TargetTitles titles = lookupTitles(rows, InteractionDO::getTargetType, InteractionDO::getTargetId);
        List<ProfileCollectVO> out = new ArrayList<>();
        for (InteractionDO i : rows) {
            String type = adapterType(i.getTargetType());
            String title = titles.title(i.getTargetType(), i.getTargetId());
            if (type == null || title == null) {
                continue;
            }
            out.add(convert.toCollectVO(i, type, title));
        }
        return out;
    }

    /** 我的评论：interaction(type=comment 且未软删) 回查来源标题，逝去来源丢弃。 */
    private List<ProfileCommentVO> buildComments(Long uid) {
        List<InteractionDO> rows = interactionDao.lambdaQuery()
                .eq(InteractionDO::getUserId, uid)
                .eq(InteractionDO::getType, TYPE_COMMENT)
                .eq(InteractionDO::getContentDeleted, NOT_DELETED)
                .orderByDesc(InteractionDO::getCreateTime)
                .last("LIMIT " + LIST_LIMIT)
                .list();
        TargetTitles titles = lookupTitles(rows, InteractionDO::getTargetType, InteractionDO::getTargetId);
        List<ProfileCommentVO> out = new ArrayList<>();
        for (InteractionDO i : rows) {
            String type = adapterType(i.getTargetType());
            String title = titles.title(i.getTargetType(), i.getTargetId());
            if (type == null || title == null) {
                continue;
            }
            out.add(convert.toCommentVO(i, type, title));
        }
        return out;
    }

    /** 浏览记录：view_log 回查目标标题，逝去目标丢弃，viewed_at 倒序。 */
    private List<ProfileHistoryVO> buildViewHistory(Long uid) {
        List<ViewLogDO> rows = viewLogMapper.selectList(new LambdaQueryWrapper<ViewLogDO>()
                .eq(ViewLogDO::getUserId, uid)
                .orderByDesc(ViewLogDO::getViewedAt)
                .last("LIMIT " + LIST_LIMIT));
        TargetTitles titles = lookupTitles(rows, ViewLogDO::getTargetType, ViewLogDO::getTargetId);
        List<ProfileHistoryVO> out = new ArrayList<>();
        for (ViewLogDO l : rows) {
            String type = adapterType(l.getTargetType());
            String title = titles.title(l.getTargetType(), l.getTargetId());
            if (type == null || title == null) {
                continue;
            }
            out.add(convert.toHistoryVO(l, type, title));
        }
        return out;
    }

    /** 我的发布：商机 + 需求合并，各含其一级评论作为 replies，按创建时间倒序取前 100。 */
    private List<ProfilePublishVO> buildPublishes(Long uid) {
        List<OpportunityDO> opps = opportunityDao.lambdaQuery()
                .eq(OpportunityDO::getPublisherId, uid)
                .orderByDesc(OpportunityDO::getCreateTime)
                .last("LIMIT " + LIST_LIMIT)
                .list();
        List<OpportunityRequestDO> reqs = requirementDao.lambdaQuery()
                .eq(OpportunityRequestDO::getPublisherId, uid)
                .orderByDesc(OpportunityRequestDO::getCreateTime)
                .last("LIMIT " + LIST_LIMIT)
                .list();

        Map<Long, List<ProfilePublishReplyVO>> oppReplies = repliesFor(TARGET_OPPORTUNITY,
                opps.stream().map(OpportunityDO::getId).collect(Collectors.toList()));
        Map<Long, List<ProfilePublishReplyVO>> reqReplies = repliesFor(TARGET_REQUEST,
                reqs.stream().map(OpportunityRequestDO::getId).collect(Collectors.toList()));

        List<ProfilePublishVO> merged = new ArrayList<>();
        for (OpportunityDO o : opps) {
            merged.add(convert.toPublishVO(o, oppReplies.getOrDefault(o.getId(), Collections.emptyList())));
        }
        for (OpportunityRequestDO r : reqs) {
            merged.add(convert.toPublishVO(r, reqReplies.getOrDefault(r.getId(), Collections.emptyList())));
        }
        // 两源合并后按创建时间倒序、null 垫底，取前 100
        return merged.stream()
                .sorted((a, b) -> compareDesc(a.getCreatedAt(), b.getCreatedAt()))
                .limit(LIST_LIMIT)
                .collect(Collectors.toList());
    }

    /** 我提交的方案：solution_response 回查所属需求，逝去需求丢弃。 */
    private List<ProfileSolutionVO> buildSolutions(Long uid) {
        List<SolutionResponseDO> sols = solutionResponseDao.lambdaQuery()
                .eq(SolutionResponseDO::getResponderId, uid)
                .orderByDesc(SolutionResponseDO::getCreateTime)
                .last("LIMIT " + LIST_LIMIT)
                .list();
        Set<Long> reqIds = sols.stream()
                .map(SolutionResponseDO::getRequestId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, OpportunityRequestDO> reqMap = reqIds.isEmpty()
                ? Collections.emptyMap()
                : requirementDao.listByIds(reqIds).stream()
                        .collect(Collectors.toMap(OpportunityRequestDO::getId, r -> r, (a, b) -> a));
        List<ProfileSolutionVO> out = new ArrayList<>();
        for (SolutionResponseDO s : sols) {
            OpportunityRequestDO req = s.getRequestId() == null ? null : reqMap.get(s.getRequestId());
            if (req == null) {   // 需求已删或缺失 → 丢弃
                continue;
            }
            out.add(convert.toSolutionVO(s, req));
        }
        return out;
    }

    // ---------- 回查辅助 ----------

    /** 某目标类型下一批 id 的一级评论（interaction），按目标 id 分组，作为 replies。 */
    private Map<Long, List<ProfilePublishReplyVO>> repliesFor(String targetType, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<InteractionDO> comments = interactionDao.lambdaQuery()
                .eq(InteractionDO::getTargetType, targetType)
                .in(InteractionDO::getTargetId, ids)
                .eq(InteractionDO::getType, TYPE_COMMENT)
                .eq(InteractionDO::getContentDeleted, NOT_DELETED)
                .isNull(InteractionDO::getParentCommentId)
                .orderByDesc(InteractionDO::getCreateTime)
                .list();
        Map<Long, List<ProfilePublishReplyVO>> grouped = new LinkedHashMap<>();
        for (InteractionDO c : comments) {
            grouped.computeIfAbsent(c.getTargetId(), k -> new ArrayList<>())
                    .add(convert.toReplyVO(c));
        }
        return grouped;
    }

    /**
     * 把一批行的 (targetType, targetId) 批量回查标题：按类型分桶，各 listByIds 一次。
     * listByIds 走 @TableLogic 过滤，逻辑删的目标不会回来，从而在调用方被丢弃。
     */
    private <T> TargetTitles lookupTitles(List<T> rows,
                                          java.util.function.Function<T, String> typeGetter,
                                          java.util.function.Function<T, Long> idGetter) {
        Set<Long> oppIds = new HashSet<>();
        Set<Long> reqIds = new HashSet<>();
        for (T row : rows) {
            String type = typeGetter.apply(row);
            Long id = idGetter.apply(row);
            if (id == null) {
                continue;
            }
            if (TARGET_OPPORTUNITY.equalsIgnoreCase(type)) {
                oppIds.add(id);
            } else if (TARGET_REQUEST.equalsIgnoreCase(type)) {
                reqIds.add(id);
            }
        }
        Map<Long, String> oppTitles = oppIds.isEmpty() ? Collections.emptyMap()
                : opportunityDao.listByIds(oppIds).stream()
                        .collect(Collectors.toMap(OpportunityDO::getId, OpportunityDO::getTitle, (a, b) -> a));
        Map<Long, String> reqTitles = reqIds.isEmpty() ? Collections.emptyMap()
                : requirementDao.listByIds(reqIds).stream()
                        .collect(Collectors.toMap(OpportunityRequestDO::getId, OpportunityRequestDO::getTitle, (a, b) -> a));
        return new TargetTitles(oppTitles, reqTitles);
    }

    /** interaction/view_log 的目标类型（Opportunity/Request）→ 前端类型（opportunity/requirement）。 */
    private String adapterType(String rawTargetType) {
        if (TARGET_OPPORTUNITY.equalsIgnoreCase(rawTargetType)) {
            return ADAPTER_OPPORTUNITY;
        }
        if (TARGET_REQUEST.equalsIgnoreCase(rawTargetType)) {
            return ADAPTER_REQUIREMENT;
        }
        return null;
    }

    /** 创建时间倒序比较，null 垫底。 */
    private int compareDesc(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return b.compareTo(a);
    }

    /** 回查结果的小载体：按目标类型取标题。 */
    private static final class TargetTitles {
        private final Map<Long, String> opp;
        private final Map<Long, String> req;

        TargetTitles(Map<Long, String> opp, Map<Long, String> req) {
            this.opp = opp;
            this.req = req;
        }

        String title(String rawTargetType, Long id) {
            if (id == null) {
                return null;
            }
            if (TARGET_OPPORTUNITY.equalsIgnoreCase(rawTargetType)) {
                return opp.get(id);
            }
            if (TARGET_REQUEST.equalsIgnoreCase(rawTargetType)) {
                return req.get(id);
            }
            return null;
        }
    }
}
