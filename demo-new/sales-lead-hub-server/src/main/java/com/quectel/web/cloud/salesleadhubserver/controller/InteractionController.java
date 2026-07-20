package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionCommentDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionLikeDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.InteractionQueryDTO;
import com.quectel.web.cloud.salesleadhubserver.service.InteractionService;
import com.quectel.web.cloud.salesleadhubserver.vo.CommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.ReactionVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 互动（评论/点赞/收藏）接口。路径与前端 {@code apis/interaction/interactionApi.ts} 的
 * url 字面量逐字一致：{@code interaction/comments}、{@code interaction/comment}、{@code interaction/like}。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无业务角色，理由见 RequirementController）；本模块
 * 登录即可、无业务角色门槛，写操作用当前登录人身份，由 service 层 {@code CurrentUserResolver} 兜底。</p>
 */
@RestController
@RequestMapping("/interaction")
public class InteractionController {

    private final InteractionService service;

    public InteractionController(InteractionService service) {
        this.service = service;
    }

    /** 查询目标对象的评论列表（≤2 级树）。前端 body {@code { targetType, targetId }}。 */
    @PostMapping("/comments")
    public Result<List<CommentVO>> comments(@Valid @RequestBody InteractionQueryDTO dto) {
        return Result.success(service.comments(dto));
    }

    /** 发表评论 / 回复。前端 body {@code { targetType, targetId, content, parentId? }}，返回 void。 */
    @PostMapping("/comment")
    public Result<Void> comment(@Valid @RequestBody InteractionCommentDTO dto) {
        service.comment(dto);
        return Result.success();
    }

    /** 点赞 / 收藏切换。前端点赞评论 body 仅 {@code { id }}；返回当前状态（前端不读，忽略）。 */
    @PostMapping("/like")
    public Result<ReactionVO> like(@Valid @RequestBody InteractionLikeDTO dto) {
        return Result.success(service.like(dto));
    }
}
