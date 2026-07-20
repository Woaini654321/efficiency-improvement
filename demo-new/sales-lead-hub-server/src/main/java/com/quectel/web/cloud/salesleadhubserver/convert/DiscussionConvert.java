package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionReplyDO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionPageVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 手写 DTO/VO ⇄ DO 映射（不引 MapStruct）。
 *
 * <p>author* 与审计字段一律不在此处赋值：前者由 service 从本地 sys_user 回填
 * （防客户端伪造），后者由框架 {@code SecurityMetaObjectHandler} 填充。</p>
 */
@Component
public class DiscussionConvert {

    public DiscussionPostDO toCreateDO(DiscussionCreateDTO dto) {
        DiscussionPostDO d = new DiscussionPostDO();
        d.setTitle(dto.getTitle());
        d.setTopic(dto.getTopic());
        d.setContent(dto.getContent());
        return d;
    }

    public DiscussionPageVO toPageVO(DiscussionPostDO d) {
        DiscussionPageVO v = new DiscussionPageVO();
        v.setPostId(d.getId());
        v.setTitle(d.getTitle());
        v.setContent(d.getContent());
        v.setTopic(d.getTopic());
        v.setAuthorName(d.getAuthorName());
        v.setReplyCount(d.getReplyCount());
        v.setViewCount(d.getViewCount());
        v.setIsHot(d.getIsHot());
        v.setTags(d.getTags());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    /**
     * 组装详情（含递归回帖树）。
     *
     * @param d       帖主表行（view_count 应为已自增后的值）
     * @param replies 该帖全部回帖（一次性全量查出，内存组树）
     */
    public DiscussionDetailVO toDetailVO(DiscussionPostDO d, List<DiscussionReplyDO> replies) {
        DiscussionDetailVO v = new DiscussionDetailVO();
        v.setPostId(d.getId());
        v.setTitle(d.getTitle());
        v.setContent(d.getContent());
        v.setTopic(d.getTopic());
        v.setAuthorName(d.getAuthorName());
        v.setReplyCount(d.getReplyCount());
        v.setViewCount(d.getViewCount());
        v.setIsHot(d.getIsHot());
        v.setTags(d.getTags());
        v.setCreatedAt(d.getCreateTime());
        v.setComments(buildCommentTree(replies));
        return v;
    }

    /**
     * 把扁平回帖行按 parent_id 自引用组装成递归树。
     *
     * <p>算法：先把每行映射成 {@link DiscussionCommentVO} 建 id→node 索引；再遍历，
     * parent_id 为 null 或<b>指向的父节点不在本集合里（孤儿）</b>的挂到根，其余挂到父节点的
     * children 下。<b>孤儿挂根策略</b>：宁可把父引用悬空的回帖提到顶层展示，也不静默丢弃
     * ——讨论区回帖是用户产出内容，丢失比错位更不可接受。同层 children 按 create_time 正序
     * （与前端「children 正序」一致）。</p>
     *
     * <p>深度不限：讨论区属下期模块，<b>不受 MOD-04「评论 ≤ 2 级」约束</b>
     * （mock 实测嵌套 ≥5 层）。</p>
     */
    public List<DiscussionCommentVO> buildCommentTree(List<DiscussionReplyDO> replies) {
        List<DiscussionCommentVO> roots = new ArrayList<>();
        if (replies == null || replies.isEmpty()) {
            return roots;
        }
        // 建索引，保留插入顺序
        Map<Long, DiscussionCommentVO> index = new LinkedHashMap<>();
        for (DiscussionReplyDO r : replies) {
            index.put(r.getId(), toCommentVO(r));
        }
        // 二次遍历挂树
        for (DiscussionReplyDO r : replies) {
            DiscussionCommentVO node = index.get(r.getId());
            Long parentId = r.getParentId();
            DiscussionCommentVO parent = parentId == null ? null : index.get(parentId);
            if (parent == null) {
                // 一级回帖，或父引用悬空的孤儿 → 挂根
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortRecursively(roots);
        return roots;
    }

    /**
     * 单条回帖 → 回帖节点（children 为空数组），供 reply 端点返回刚落库的新节点。
     *
     * <p>刚插入的回帖必然无子节点，故 children 保持默认空数组；created_at 取自 DO 的
     * create_time（由框架 fill=INSERT 在 save 后回填到实体）。</p>
     */
    public DiscussionCommentVO toReplyNode(DiscussionReplyDO r) {
        return toCommentVO(r);
    }

    private DiscussionCommentVO toCommentVO(DiscussionReplyDO r) {
        DiscussionCommentVO c = new DiscussionCommentVO();
        c.setCommentId(r.getId());
        c.setAuthorName(r.getAuthorName());
        c.setContent(r.getContent());
        c.setCreatedAt(r.getCreateTime());
        return c;
    }

    /** 每层 children 按 create_time 正序（null 时间排最后，避免 NPE）。 */
    private void sortRecursively(List<DiscussionCommentVO> nodes) {
        nodes.sort(Comparator.comparing(DiscussionCommentVO::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        for (DiscussionCommentVO n : nodes) {
            sortRecursively(n.getChildren());
        }
    }

    // 显式逐字段赋值、两个 toXxxVO 不抽公共基类：契约类型保持扁平可读，
    // 任一 VO 增减字段时编译器能直接指到这里。
}
