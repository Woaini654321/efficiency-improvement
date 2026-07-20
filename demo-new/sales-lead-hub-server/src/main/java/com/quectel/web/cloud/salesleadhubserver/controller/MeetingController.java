package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingCancelDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingUrgeDTO;
import com.quectel.web.cloud.salesleadhubserver.service.MeetingService;
import com.quectel.web.cloud.salesleadhubserver.vo.MeetingTaskPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 会议任务接口。路径与前端 {@code apis/meeting/meetingApi.ts} 一一对应。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无业务角色，理由见 RequirementController/
 * CurrentUserResolver）；业务角色与 owner 由 service 层查本地 sys_user 判定。</p>
 */
@RestController
@RequestMapping("/meeting")
public class MeetingController {

    private final MeetingService service;

    public MeetingController(MeetingService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<MeetingTaskPageVO>> page(@RequestBody MeetingPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody MeetingCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody MeetingUpdateDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("/urge")
    public Result<Void> urge(@Valid @RequestBody MeetingUrgeDTO dto) {
        service.urge(dto.getId(), dto.getRemark());
        return Result.success();
    }

    @PostMapping("/cancel")
    public Result<Void> cancel(@Valid @RequestBody MeetingCancelDTO dto) {
        service.cancel(dto.getId(), dto.getReason());
        return Result.success();
    }
}
