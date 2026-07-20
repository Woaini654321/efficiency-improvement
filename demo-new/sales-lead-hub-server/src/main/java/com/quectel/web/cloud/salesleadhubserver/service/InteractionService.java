package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.InteractionCommentDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionLikeDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionQueryDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.CommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ReactionVO;

import java.util.List;

/** 互动（评论/点赞/收藏）业务。登录即可，无业务角色门槛；写操作一律用当前登录人身份。 */
public interface InteractionService {

    /** 查询目标对象的评论列表（≤2 级树，正序）。 */
    List<CommentVO> comments(InteractionQueryDTO dto);

    /** 发表评论 / 回复，返回新评论 id。同事务回写目标表 comment_count。 */
    Long comment(InteractionCommentDTO dto);

    /** 点赞 / 收藏幂等切换：未反应则新增，已反应则取消。带计数列的目标同事务原子回写。 */
    ReactionVO like(InteractionLikeDTO dto);
}
