package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.InteractionConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.InteractionDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SolutionResponseDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionCommentDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionLikeDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionQueryDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.InteractionErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.InteractionService;
import com.quectel.web.cloud.salesleadhubserver.vo.CommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ReactionVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InteractionServiceImpl implements InteractionService {

    private static final String TYPE_COMMENT = "comment";
    private static final String TYPE_LIKE = "like";
    private static final String TYPE_COLLECT = "collect";

    private static final String TARGET_OPPORTUNITY = "Opportunity";
    private static final String TARGET_REQUEST = "Request";
    private static final String TARGET_RESPONSE = "Response";
    private static final String TARGET_COMMENT = "Comment";

    /** 评论正文长度上限，对齐前端 a-textarea maxlength=500（TEXT 列本身够大，前端限制更紧）。 */
    private static final int MAX_COMMENT_LEN = 500;

    private final InteractionDao dao;
    private final OpportunityDao opportunityDao;
    private final RequirementDao requirementDao;
    private final SolutionResponseDao solutionResponseDao;
    private final SysUserDao sysUserDao;
    private final CurrentUserResolver currentUser;
    private final InteractionConvert convert;

    public InteractionServiceImpl(InteractionDao dao,
                                  OpportunityDao opportunityDao,
                                  RequirementDao requirementDao,
                                  SolutionResponseDao solutionResponseDao,
                                  SysUserDao sysUserDao,
                                  CurrentUserResolver currentUser,
                                  InteractionConvert convert) {
        this.dao = dao;
        this.opportunityDao = opportunityDao;
        this.requirementDao = requirementDao;
        this.solutionResponseDao = solutionResponseDao;
        this.sysUserDao = sysUserDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    // ==================== 查询：评论树 ====================

    @Override
    @Transactional(readOnly = true)
    public List<CommentVO> comments(InteractionQueryDTO dto) {
        String targetType = dto.getTargetType();
        Long targetId = parseId(dto.getTargetId(), "目标 id 非法");

        // 该目标下所有评论行，按时间正序（与前端 mock 展示顺序一致）
        List<InteractionDO> all = dao.list(Wrappers.<InteractionDO>lambdaQuery()
                .eq(InteractionDO::getTargetType, targetType)
                .eq(InteractionDO::getTargetId, targetId)
                .eq(InteractionDO::getType, TYPE_COMMENT)
                .orderByAsc(InteractionDO::getCreateTime));
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // 部门名：interaction 表无部门快照列，按 user_id 批量联本地 sys_user 取（无快照则反映当前部门）
        Map<Long, String> deptByUser = resolveDeptNames(all);
        // 点赞数：按 target_type=Comment + target_id∈评论 id 集 + type=like 批量派生
        Map<Long, Integer> likeByComment = resolveCommentLikeCounts(all);

        // 组装两级树：一级按时间正序，回复挂到其父下（同样正序）
        Map<Long, CommentVO> topById = new LinkedHashMap<>();
        List<InteractionDO> replies = new ArrayList<>();
        for (InteractionDO d : all) {
            int likes = likeByComment.getOrDefault(d.getId(), 0);
            String dept = deptByUser.get(d.getUserId());
            if (d.getParentCommentId() == null) {
                topById.put(d.getId(), convert.toCommentVO(d, dept, likes));
            } else {
                replies.add(d);
            }
        }
        for (InteractionDO r : replies) {
            CommentVO parent = topById.get(r.getParentCommentId());
            if (parent == null) {
                // 父不在本目标（脏数据）：跳过，不制造悬挂节点
                continue;
            }
            int likes = likeByComment.getOrDefault(r.getId(), 0);
            parent.getReplies().add(convert.toCommentVO(r, deptByUser.get(r.getUserId()), likes));
        }
        return new ArrayList<>(topById.values());
    }

    // ==================== 发表评论 / 回复 ====================

    @Override
    @Transactional
    public Long comment(InteractionCommentDTO dto) {
        SysUserDO me = requireCurrentUser();

        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty()) {
            throw new BaseException(InteractionErrorCode.COMMENT_CONTENT_REQUIRED, "评论内容不能为空");
        }
        if (content.length() > MAX_COMMENT_LEN) {
            throw new BaseException(InteractionErrorCode.COMMENT_CONTENT_TOO_LONG,
                    "评论内容不能超过 " + MAX_COMMENT_LEN + " 字");
        }

        String targetType = dto.getTargetType();
        Long targetId = parseId(dto.getTargetId(), "目标 id 非法");
        requireContentTargetExists(targetType, targetId);

        Long parentId = null;
        if (dto.getParentId() != null && !dto.getParentId().isEmpty()) {
            parentId = parseId(dto.getParentId(), "父评论 id 非法");
            requireValidParent(parentId, targetType, targetId);
        }

        InteractionDO d = new InteractionDO();
        d.setUserId(me.getId());
        d.setUserName(me.getName());           // 姓名快照取本地 sys_user
        d.setTargetType(targetType);
        d.setTargetId(targetId);
        d.setType(TYPE_COMMENT);
        d.setContent(content);
        d.setParentCommentId(parentId);
        d.setContentDeleted(0);
        dao.save(d);

        // 计数回写：一级评论与回复都计入（与前端 totalCommentCount 口径一致）
        adjustTargetCount(targetType, targetId, TYPE_COMMENT, 1);
        return d.getId();
    }

    // ==================== 点赞 / 收藏 幂等切换 ====================

    @Override
    @Transactional
    public ReactionVO like(InteractionLikeDTO dto) {
        SysUserDO me = requireCurrentUser();

        Long targetId = parseId(dto.getId(), "目标 id 非法");
        // 前端点赞评论只传 id，targetType 缺省 Comment、type 缺省 like
        String targetType = (dto.getTargetType() == null || dto.getTargetType().isEmpty())
                ? TARGET_COMMENT : dto.getTargetType();
        String type = (dto.getType() == null || dto.getType().isEmpty())
                ? TYPE_LIKE : dto.getType();
        if (!TYPE_LIKE.equals(type) && !TYPE_COLLECT.equals(type)) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "反应类型只能是 like/collect：" + type);
        }
        requireReactionTargetExists(targetType, targetId);

        boolean liked;
        try {
            // 先尝试 insert；唯一键 uk_inter_reaction 命中则说明已反应过 → 取消
            InteractionDO r = new InteractionDO();
            r.setUserId(me.getId());
            r.setUserName(me.getName());
            r.setTargetType(targetType);
            r.setTargetId(targetId);
            r.setType(type);
            r.setContentDeleted(0);
            dao.save(r);
            liked = true;
            adjustTargetCount(targetType, targetId, type, 1);
        } catch (DuplicateKeyException e) {
            // 已存在 → 按 reaction 维度物理删该行 = 取消（MySQL 唯一键冲突只回滚该语句，
            // 同事务后续 remove/update 仍可执行；捕获在方法内不会把外层事务标记为 rollback-only）
            dao.remove(reactionWrapper(me.getId(), targetType, targetId, type));
            liked = false;
            adjustTargetCount(targetType, targetId, type, -1);
        }

        long count = dao.count(Wrappers.<InteractionDO>lambdaQuery()
                .eq(InteractionDO::getTargetType, targetType)
                .eq(InteractionDO::getTargetId, targetId)
                .eq(InteractionDO::getType, type));
        return new ReactionVO(liked, (int) count);
    }

    // ==================== private ====================

    /** 登录人本地档案；未开通抛 FORBIDDEN（本模块无业务角色门槛，故不用 requireAnyRole）。 */
    private SysUserDO requireCurrentUser() {
        SysUserDO me = currentUser.currentOrNull();
        if (me == null) {
            throw new BaseException(ErrorCode.FORBIDDEN,
                    "当前账号尚未在本平台开通，请联系运营维护用户档案");
        }
        return me;
    }

    private Long parseId(String raw, String message) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, message + "：" + raw);
        }
    }

    /** 评论目标只能是内容主体（Opportunity/Request/Response），不能对评论本身发评论。 */
    private void requireContentTargetExists(String targetType, Long targetId) {
        boolean exists;
        switch (targetType) {
            case TARGET_OPPORTUNITY:
                exists = opportunityDao.getById(targetId) != null;
                break;
            case TARGET_REQUEST:
                exists = requirementDao.getById(targetId) != null;
                break;
            case TARGET_RESPONSE:
                exists = solutionResponseDao.getById(targetId) != null;
                break;
            default:
                throw new BaseException(InteractionErrorCode.UNSUPPORTED_TARGET_TYPE,
                        "评论目标类型不支持：" + targetType);
        }
        if (!exists) {
            throw new BaseException(ErrorCode.NOT_FOUND, "评论目标不存在");
        }
    }

    /** 反应目标：内容主体或评论（点赞评论时 target=Comment）。 */
    private void requireReactionTargetExists(String targetType, Long targetId) {
        boolean exists;
        switch (targetType) {
            case TARGET_OPPORTUNITY:
                exists = opportunityDao.getById(targetId) != null;
                break;
            case TARGET_REQUEST:
                exists = requirementDao.getById(targetId) != null;
                break;
            case TARGET_RESPONSE:
                exists = solutionResponseDao.getById(targetId) != null;
                break;
            case TARGET_COMMENT:
                InteractionDO c = dao.getById(targetId);
                exists = c != null && TYPE_COMMENT.equals(c.getType());
                break;
            default:
                throw new BaseException(InteractionErrorCode.UNSUPPORTED_TARGET_TYPE,
                        "反应目标类型不支持：" + targetType);
        }
        if (!exists) {
            throw new BaseException(ErrorCode.NOT_FOUND, "反应目标不存在");
        }
    }

    /**
     * 校验回复目标：parentId 必须指向<b>同一目标下的一条一级评论</b>（自身 parentCommentId 为空、
     * type=comment）。否则拒绝——挡住三级回复与跨目标挂靠。
     */
    private void requireValidParent(Long parentId, String targetType, Long targetId) {
        InteractionDO parent = dao.getById(parentId);
        boolean valid = parent != null
                && TYPE_COMMENT.equals(parent.getType())
                && parent.getParentCommentId() == null           // 一级评论
                && targetType.equals(parent.getTargetType())
                && targetId.equals(parent.getTargetId());
        if (!valid) {
            throw new BaseException(InteractionErrorCode.INVALID_PARENT_COMMENT,
                    "回复只能挂在同一对象下的一级评论上（评论至多 2 级）");
        }
    }

    /**
     * 目标表计数原子回写：{@code col = GREATEST(col ± 1, 0)}，禁读出+1再写回（并发丢计数）。
     *
     * <p>只有带计数列的 Opportunity / Request 才回写；Response 表无计数列、Comment 的点赞按行 COUNT
     * 派生，二者均跳过。用 {@code IService.update(Wrapper)} 单参重载，SET 子句由 setSql 提供。</p>
     */
    private void adjustTargetCount(String targetType, Long targetId, String type, int delta) {
        String column = columnOf(type);
        if (column == null) {
            return;
        }
        String setSql = column + " = GREATEST(" + column + (delta > 0 ? " + 1" : " - 1") + ", 0)";
        if (TARGET_OPPORTUNITY.equals(targetType)) {
            LambdaUpdateWrapper<OpportunityDO> uw = Wrappers.<OpportunityDO>lambdaUpdate()
                    .setSql(setSql)
                    .eq(OpportunityDO::getId, targetId);
            opportunityDao.update(uw);
        } else if (TARGET_REQUEST.equals(targetType)) {
            LambdaUpdateWrapper<OpportunityRequestDO> uw = Wrappers.<OpportunityRequestDO>lambdaUpdate()
                    .setSql(setSql)
                    .eq(OpportunityRequestDO::getId, targetId);
            requirementDao.update(uw);
        }
        // TARGET_RESPONSE / TARGET_COMMENT：无计数列，跳过
    }

    private String columnOf(String type) {
        switch (type) {
            case TYPE_LIKE:
                return "like_count";
            case TYPE_COLLECT:
                return "collect_count";
            case TYPE_COMMENT:
                return "comment_count";
            default:
                return null;
        }
    }

    private LambdaQueryWrapper<InteractionDO> reactionWrapper(Long userId, String targetType,
                                                              Long targetId, String type) {
        return Wrappers.<InteractionDO>lambdaQuery()
                .eq(InteractionDO::getUserId, userId)
                .eq(InteractionDO::getTargetType, targetType)
                .eq(InteractionDO::getTargetId, targetId)
                .eq(InteractionDO::getType, type);
    }

    /** 批量取评论人部门名（按 user_id 联本地 sys_user）。 */
    private Map<Long, String> resolveDeptNames(List<InteractionDO> comments) {
        List<Long> userIds = comments.stream()
                .map(InteractionDO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new java.util.HashMap<>();
        for (SysUserDO u : sysUserDao.listByIds(userIds)) {
            map.put(u.getId(), u.getDepartmentName());
        }
        return map;
    }

    /** 批量派生每条评论的点赞数（target_type=Comment + target_id∈ids + type=like）。 */
    private Map<Long, Integer> resolveCommentLikeCounts(List<InteractionDO> comments) {
        List<Long> ids = comments.stream()
                .map(InteractionDO::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<InteractionDO> likes = dao.list(Wrappers.<InteractionDO>lambdaQuery()
                .eq(InteractionDO::getTargetType, TARGET_COMMENT)
                .eq(InteractionDO::getType, TYPE_LIKE)
                .in(InteractionDO::getTargetId, ids));
        Map<Long, Integer> map = new java.util.HashMap<>();
        for (InteractionDO l : likes) {
            map.merge(l.getTargetId(), 1, Integer::sum);
        }
        return map;
    }
}
