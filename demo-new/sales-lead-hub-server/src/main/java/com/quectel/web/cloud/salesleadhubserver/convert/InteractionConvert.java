package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;
import com.quectel.web.cloud.salesleadhubserver.vo.CommentVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 手写评论 DO→VO 映射，不引 MapStruct。
 *
 * <p>只负责单节点转换与软删占位；两级树的组装（父挂子、点赞计数、部门名注入）在 service。</p>
 */
@Component
public class InteractionConvert {

    /** content_deleted=1 时的占位文案（前端 mock 无占位样例，取 PRD D7 默认口径）。 */
    public static final String DELETED_PLACEHOLDER = "该评论已删除";

    /**
     * 单条评论 → VO。
     *
     * @param d          评论行
     * @param authorDept 评论人部门名（interaction 表无部门快照列，由 service 联 sys_user 注入）
     * @param likeCount  该评论的点赞数（按 reaction 行派生，由 service 注入）
     */
    public CommentVO toCommentVO(InteractionDO d, String authorDept, int likeCount) {
        CommentVO v = new CommentVO();
        v.setInteractionId(d.getId());
        v.setTargetType(d.getTargetType());
        v.setTargetId(d.getTargetId());
        v.setAuthorName(d.getUserName());
        v.setAuthorDept(authorDept);
        // D7 软删：行保留、内容转占位（子回复由 service 照常挂上）
        boolean deleted = d.getContentDeleted() != null && d.getContentDeleted() == 1;
        v.setContent(deleted ? DELETED_PLACEHOLDER : d.getContent());
        v.setLikeCount(likeCount);
        v.setParentId(d.getParentCommentId());
        v.setCreatedAt(d.getCreateTime());
        v.setReplies(new ArrayList<>());
        return v;
    }
}
