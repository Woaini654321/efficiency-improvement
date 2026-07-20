package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.FeedbackCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.IdDTO;
import com.quectel.web.cloud.salesleadhubserver.service.FeedbackService;
import com.quectel.web.cloud.salesleadhubserver.vo.FeedbackVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 吐槽墙接口。路径与前端 {@code apis/feedback/feedbackApi.ts} 一一对应
 * （feedback/list、feedback/create、feedback/like）。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无业务角色，理由见 RequirementController）；
 * 登录门槛由 service 层 {@code CurrentUserResolver} 判定（登录即可，无 admin 门槛）。</p>
 *
 * <p>list 返回 {@code {records,total}} 结构：前端 feedbackAdapter 读 {@code data.records}，
 * total 冗余但无害。</p>
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService service;

    public FeedbackController(FeedbackService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Result<PageVO<FeedbackVO>> list() {
        return Result.success(service.list());
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody FeedbackCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/like")
    public Result<Void> like(@Valid @RequestBody IdDTO dto) {
        service.like(dto.getId());
        return Result.success();
    }
}
