package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SolutionResponseDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCollectVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileHistoryVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfilePublishReplyVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfilePublishVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileSolutionVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileStatsVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileUserVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SubscriptionNodeVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 个人中心手写映射：用户段脱敏 + 角色码转中文名、统计段装配、分类森林组装，
 * 以及收藏/发布/方案/评论/浏览五段列表的单行映射（回查得到的标题/类型由 service 传入）。
 */
@Component
public class ProfileConvert {

    private static final String TYPE_OPPORTUNITY = "opportunity";
    private static final String TYPE_REQUIREMENT = "requirement";
    private static final String STATUS_PUBLISHED = "published";

    // ---------- 用户段 / 统计段 / 分类森林 ----------

    /** 当前登录人档案 → 用户段。刻意不搬运 phone（页面不展示，直接不返回即最稳脱敏）。 */
    public ProfileUserVO toUserVO(SysUserDO me) {
        ProfileUserVO v = new ProfileUserVO();
        v.setName(me.getName());
        v.setDeptName(me.getDepartmentName());
        v.setRoleName(roleName(me.getRole()));
        v.setEmployeeNo(me.getEmployeeId());
        return v;
    }

    /** 6 个计数装配（long→Integer，前端契约是 number）。 */
    public ProfileStatsVO toStatsVO(long collect, long comment, long publish,
                                    long solution, long draft, long view) {
        ProfileStatsVO v = new ProfileStatsVO();
        v.setCollectCount((int) collect);
        v.setCommentCount((int) comment);
        v.setPublishCount((int) publish);
        v.setSolutionCount((int) solution);
        v.setDraftCount((int) draft);
        v.setViewCount((int) view);
        return v;
    }

    /**
     * 分类行 → 森林（对齐前端 SubscriptionNode：title/value/key/children）。
     *
     * <p>value/key 用分类 id 字符串；按 parentId 组树，同级按 sortOrder 升序。
     * 叶子 children 留 null（全局 NON_NULL 省略该键，前端有兜底）。</p>
     */
    public List<SubscriptionNodeVO> buildForest(List<CategoryDO> categories) {
        Map<Long, List<CategoryDO>> byParent = new LinkedHashMap<>();
        for (CategoryDO c : categories) {
            long parent = c.getParentId() == null ? 0L : c.getParentId();
            byParent.computeIfAbsent(parent, k -> new ArrayList<>()).add(c);
        }
        return buildChildren(0L, byParent);
    }

    private List<SubscriptionNodeVO> buildChildren(Long parentKey, Map<Long, List<CategoryDO>> byParent) {
        List<CategoryDO> kids = byParent.get(parentKey);
        if (kids == null || kids.isEmpty()) {
            return new ArrayList<>();
        }
        return kids.stream()
                .sorted(Comparator.comparing(c -> c.getSortOrder() == null ? 0 : c.getSortOrder()))
                .map(c -> {
                    SubscriptionNodeVO n = new SubscriptionNodeVO();
                    n.setTitle(c.getName());
                    n.setValue(String.valueOf(c.getId()));
                    n.setKey(String.valueOf(c.getId()));
                    List<SubscriptionNodeVO> children = buildChildren(c.getId(), byParent);
                    n.setChildren(children.isEmpty() ? null : children);
                    return n;
                })
                .collect(Collectors.toList());
    }

    // ---------- 五段列表单行映射 ----------

    /** 收藏行（回查得到的 adapterType 与 title 由 service 传入）。 */
    public ProfileCollectVO toCollectVO(InteractionDO i, String type, String title) {
        ProfileCollectVO v = new ProfileCollectVO();
        v.setCollectId(i.getId());
        v.setTitle(title);
        v.setType(type);
        v.setIsDeleted(false);   // 回查得到即存活（删掉的已在 service 丢弃）
        v.setCreatedAt(i.getCreateTime());
        return v;
    }

    /** 评论行。 */
    public ProfileCommentVO toCommentVO(InteractionDO i, String sourceType, String sourceTitle) {
        ProfileCommentVO v = new ProfileCommentVO();
        v.setCommentId(i.getId());
        v.setContent(i.getContent());
        v.setSourceTitle(sourceTitle);
        v.setSourceType(sourceType);
        v.setSourceId(i.getTargetId());
        v.setIsDeleted(false);
        v.setCreatedAt(i.getCreateTime());
        return v;
    }

    /** 浏览记录行。 */
    public ProfileHistoryVO toHistoryVO(ViewLogDO l, String type, String title) {
        ProfileHistoryVO v = new ProfileHistoryVO();
        v.setHistoryId(l.getId());
        v.setTitle(title);
        v.setType(type);
        v.setViewedAt(l.getViewedAt());
        return v;
    }

    /** 一条评论 → 我发布内容下的回复。 */
    public ProfilePublishReplyVO toReplyVO(InteractionDO i) {
        ProfilePublishReplyVO v = new ProfilePublishReplyVO();
        v.setReplyId(i.getId());
        v.setContent(i.getContent());
        v.setFromName(i.getUserName());
        v.setRepliedAt(i.getCreateTime());
        return v;
    }

    /** 我发布的商机 → 发布项（商机无采纳概念 is_adopted 恒 false）。 */
    public ProfilePublishVO toPublishVO(OpportunityDO o, List<ProfilePublishReplyVO> replies) {
        ProfilePublishVO v = new ProfilePublishVO();
        v.setOpportunityId(o.getId());
        v.setTitle(o.getTitle());
        v.setType(TYPE_OPPORTUNITY);
        v.setStatus(o.getStatus());
        v.setViewCount(o.getViewCount());
        v.setLikeCount(o.getLikeCount());
        v.setCommentCount(o.getCommentCount());
        v.setCollectCount(o.getCollectCount());
        v.setCreatedAt(o.getCreateTime());
        v.setEditedAt(o.getUpdateTime());
        v.setIsAdopted(false);
        v.setReplies(replies);
        return v;
    }

    /** 我发布的需求 → 发布项（需求无草稿态统一按 published 呈现；is_adopted=已选采纳方案）。 */
    public ProfilePublishVO toPublishVO(OpportunityRequestDO r, List<ProfilePublishReplyVO> replies) {
        ProfilePublishVO v = new ProfilePublishVO();
        v.setOpportunityId(r.getId());
        v.setTitle(r.getTitle());
        v.setType(TYPE_REQUIREMENT);
        v.setStatus(STATUS_PUBLISHED);
        v.setViewCount(r.getViewCount());
        v.setLikeCount(r.getLikeCount());
        v.setCommentCount(r.getCommentCount());
        v.setCollectCount(r.getCollectCount());
        v.setCreatedAt(r.getCreateTime());
        v.setEditedAt(r.getUpdateTime());
        v.setIsAdopted(r.getAdoptedResponseId() != null);
        v.setReplies(replies);
        return v;
    }

    /** 我提交的方案 → 方案项（title/summary 由富文本 content 派生；采纳信息取所属需求）。 */
    public ProfileSolutionVO toSolutionVO(SolutionResponseDO s, OpportunityRequestDO req) {
        ProfileSolutionVO v = new ProfileSolutionVO();
        v.setSolutionId(s.getId());
        v.setTitle(stripAndTruncate(s.getContent(), 40));
        v.setSummary(stripAndTruncate(s.getContent(), 200));
        v.setRequestTitle(req.getTitle());
        v.setRequestId(req.getId());
        boolean best = req.getAdoptedResponseId() != null && req.getAdoptedResponseId().equals(s.getId());
        v.setIsBest(best);
        // 采纳发生在需求侧：采纳人=需求发布人，采纳时间用需求最后更新时间（无独立采纳时间列）
        v.setAdopterName(best ? req.getPublisherName() : null);
        v.setAdopterDeptName(best ? req.getPublisherDeptName() : null);
        v.setAdoptedAt(best ? req.getUpdateTime() : null);
        return v;
    }

    // ---------- 私有 ----------

    /** sys_user.role 码值 → 可读中文名（页面直接渲染不再 t() 翻译）。 */
    private String roleName(String role) {
        if (CurrentUserResolver.ROLE_ADMIN.equals(role)) {
            return "管理员";
        }
        if (CurrentUserResolver.ROLE_PRODUCT_MANAGER.equals(role)) {
            return "产品经理";
        }
        if (CurrentUserResolver.ROLE_SALES.equals(role)) {
            return "销售";
        }
        return role == null ? "" : role;
    }

    /** 富文本去标签 + 压空白后截断（solution_response 无 title/summary 列，二者据此派生）。 */
    private String stripAndTruncate(String html, int max) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
