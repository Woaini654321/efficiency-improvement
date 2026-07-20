package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionReplyDTO;
import com.quectel.web.cloud.salesleadhubserver.service.DiscussionService;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 讨论区接口。路径与前端 {@code apis/discussion/discussionApi.ts} 的 url 字面量一一对应：
 * {@code discussion/page}（POST）、{@code discussion/detail}（GET id）、{@code discussion/create}（POST）、
 * {@code discussion/reply}（POST）。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 写操作的业务角色由 service 层 {@code CurrentUserResolver} 查本地 sys_user.role 判定，读操作
 * 由 SSO TokenValidationFilter 保证已登录即可访问。</p>
 *
 * <p>{@code reply} 端点持久化回帖：写 discussion_reply + 同事务 reply_count 原子 +1，返回新回帖
 * 节点（含新 id/author_name/content/created_at/children 空数组），供前端就地插入到评论树。</p>
 */
@RestController
@RequestMapping("/discussion")
public class DiscussionController {

    private final DiscussionService service;

    public DiscussionController(DiscussionService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<DiscussionPageVO>> page(@RequestBody DiscussionPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @GetMapping("/detail")
    public Result<DiscussionDetailVO> detail(@RequestParam("id") Long id) {
        return Result.success(service.detail(id));
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody DiscussionCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/reply")
    public Result<DiscussionCommentVO> reply(@Valid @RequestBody DiscussionReplyDTO dto) {
        return Result.success(service.reply(dto));
    }
}
