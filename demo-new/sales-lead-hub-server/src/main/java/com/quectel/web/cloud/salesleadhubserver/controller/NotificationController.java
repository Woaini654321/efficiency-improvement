package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.IdDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.NotificationPreferenceDTO;
import com.quectel.web.cloud.salesleadhubserver.service.NotificationService;
import com.quectel.web.cloud.salesleadhubserver.vo.NotificationPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 通知接口。路径与前端 {@code apis/notification/notificationApi.ts} 的 url 字面量逐字一致：
 * {@code notification/page}、{@code notification/markRead}、{@code notification/markAllRead}、
 * {@code notification/preference/save}。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无业务角色，理由见 OpportunityController）；
 * 但通知不同于内容接口——它无需业务角色，任何登录人都能看/改<b>自己的</b>通知，
 * 归属边界由 service 层用 {@code SecurityUtils.getCurrentUserId()} 强制，不接受 userId 入参。</p>
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<NotificationPageVO>> page(@RequestBody NotificationPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @PostMapping("/markRead")
    public Result<Void> markRead(@Valid @RequestBody IdDTO dto) {
        service.markRead(dto.getId());
        return Result.success();
    }

    @PostMapping("/markAllRead")
    public Result<Void> markAllRead() {
        service.markAllRead();
        return Result.success();
    }

    @PostMapping("/preference/save")
    public Result<Void> savePreference(@Valid @RequestBody NotificationPreferenceDTO dto) {
        service.savePreference(dto);
        return Result.success();
    }
}
