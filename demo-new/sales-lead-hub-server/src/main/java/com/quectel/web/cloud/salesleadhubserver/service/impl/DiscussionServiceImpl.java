package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.DiscussionConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionPostDao;
import com.quectel.web.cloud.salesleadhubserver.dao.DiscussionReplyDao;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionReplyDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.DiscussionErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionReplyDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.DiscussionService;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 讨论区业务实现。
 *
 * <p><b>回帖深度不限</b>：detail 从 discussion_reply 按 post_id 全量查出后<b>内存组树</b>
 * （parent_id 自引用）。讨论区属下期模块，<b>不受 MOD-04「评论 ≤ 2 级」约束</b>——那条
 * 约束只作用于 interaction 模块嵌入商机/需求页的评论。mock 实测嵌套 ≥5 层，此处照实还原。</p>
 *
 * <p><b>回帖持久化</b>（{@link #reply}）：前端 detail 页去 mock 后，回帖不再只在内存插入
 * {@code LOCAL-*} 节点，而是落库 discussion_reply 并在<b>同一事务</b>内把 discussion_post.reply_count
 * 原子 +1。parentId 非空时校验其确属同一帖（不限层级，与组树深度不限一致）。</p>
 */
@Service
public class DiscussionServiceImpl implements DiscussionService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    /** 合法话题枚举（与 PRD §10 / 前端 a-segmented 选项一致）。 */
    private static final Set<String> TOPICS = new HashSet<>(Arrays.asList(
            "business", "solution", "experience", "industry", "complaint"));

    private final DiscussionPostDao postDao;
    private final DiscussionReplyDao replyDao;
    private final CurrentUserResolver currentUser;
    private final DiscussionConvert convert;

    public DiscussionServiceImpl(DiscussionPostDao postDao,
                                 DiscussionReplyDao replyDao,
                                 CurrentUserResolver currentUser,
                                 DiscussionConvert convert) {
        this.postDao = postDao;
        this.replyDao = replyDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<DiscussionPageVO> page(DiscussionPageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        String kw = trimToNull(dto.getKeyword());
        String topic = trimToNull(dto.getTopic());

        Page<DiscussionPostDO> p = new Page<>(pageNumber, pageSize);
        IPage<DiscussionPostDO> r = postDao.lambdaQuery()
                .eq(topic != null, DiscussionPostDO::getTopic, topic)
                // keyword 模糊匹配 title OR content
                .and(kw != null, w -> w.like(DiscussionPostDO::getTitle, kw)
                        .or().like(DiscussionPostDO::getContent, kw))
                // 热帖优先，再按发布时间倒序
                .orderByDesc(DiscussionPostDO::getIsHot)
                .orderByDesc(DiscussionPostDO::getCreateTime)
                .page(p);

        List<DiscussionPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public DiscussionDetailVO detail(Long id) {
        DiscussionPostDO d = postDao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "讨论帖不存在");
        }
        // 浏览数原子自增（SET view_count = view_count + 1），并让本次返回体现自增后的值
        postDao.increaseViewCount(id);
        d.setViewCount((d.getViewCount() == null ? 0 : d.getViewCount()) + 1);

        List<DiscussionReplyDO> replies = replyDao.list(
                new LambdaQueryWrapper<DiscussionReplyDO>()
                        .eq(DiscussionReplyDO::getPostId, id));
        return convert.toDetailVO(d, replies == null ? Collections.emptyList() : replies);
    }

    @Override
    @Transactional
    public Long create(DiscussionCreateDTO dto) {
        // 登录即可（三业务角色任一），fail-closed；未开通本地档案的账号会被拒
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        if (!TOPICS.contains(dto.getTopic())) {
            throw new BaseException(DiscussionErrorCode.INVALID_TOPIC, "非法话题：" + dto.getTopic());
        }

        DiscussionPostDO d = convert.toCreateDO(dto);
        // 作者快照取自本地 sys_user 同一行（防客户端伪造，理由见 OpportunityServiceImpl.fillPublisher）
        d.setAuthorId(me.getId());
        d.setAuthorName(me.getName());
        // tags 前端不采集，落空数组；计数与热帖默认值
        d.setTags(Collections.emptyList());
        d.setReplyCount(0);
        d.setViewCount(0);
        d.setIsHot(false);

        postDao.save(d);
        return d.getId();
    }

    @Override
    @Transactional
    public DiscussionCommentVO reply(DiscussionReplyDTO dto) {
        // 登录即可（三业务角色任一），fail-closed；未开通本地档案的账号会被拒
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);

        // 帖必须存在
        DiscussionPostDO post = postDao.getById(dto.getPostId());
        if (post == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "讨论帖不存在");
        }

        // parentId 非空时：父回帖必须存在且属于同一帖（不限层级，只校验归属不校验深度）
        if (dto.getParentId() != null) {
            DiscussionReplyDO parent = replyDao.getById(dto.getParentId());
            if (parent == null || !dto.getPostId().equals(parent.getPostId())) {
                throw new BaseException(DiscussionErrorCode.INVALID_PARENT_REPLY,
                        "父回帖不存在或不属于本帖");
            }
        }

        // 落库：作者快照取自本地 sys_user 同一行（防伪造）；create_time 由框架 fill=INSERT
        // 在 save 后回填到实体，故此处不手动赋值（DiscussionReplyDO.createTime 已标注 fill=INSERT）
        DiscussionReplyDO reply = new DiscussionReplyDO();
        reply.setPostId(dto.getPostId());
        reply.setParentId(dto.getParentId());
        reply.setAuthorId(me.getId());
        reply.setAuthorName(me.getName());
        reply.setContent(dto.getContent());
        replyDao.save(reply);

        // 同事务原子 reply_count + 1（SET reply_count = reply_count + 1）
        postDao.increaseReplyCount(dto.getPostId());

        // 返回刚落库的新节点：children 空数组，created_at 取回填后的 create_time
        return convert.toReplyNode(reply);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
