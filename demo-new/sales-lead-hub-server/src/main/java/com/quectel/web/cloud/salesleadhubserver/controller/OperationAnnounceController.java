package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceChangeStatusDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.IdDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OperationAnnouncePageDTO;
import com.quectel.web.cloud.salesleadhubserver.service.AnnouncementService;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnounceStatsVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnounceDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnouncePageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 运营端公告接口。路径与前端 {@code apis/announce/announceApi.ts} 一一对应
 * （{@code operation/announce/{page,detail,create,update,delete,changeStatus,stats}}）。
 *
 * <p><b>刻意没有 {@code @PreAuthorize}</b>（理由同 {@link AnnouncementController}：UAA 无本平台业务角色）。
 * 运营端全部端点在 service 层 {@code requireAnyRole(ROLE_ADMIN)} fail-closed 鉴权，
 * 非 admin 抛 {@code FORBIDDEN}。</p>
 *
 * <p>与前台 {@link AnnouncementController} 分成两个类：运营返回全部状态（含草稿）且有写端点，
 * 前台只读已发布，两套 VO 不互相继承。</p>
 */
@RestController
@RequestMapping("/operation/announce")
public class OperationAnnounceController {

    private final AnnouncementService service;

    public OperationAnnounceController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<OperationAnnouncePageVO>> page(@RequestBody OperationAnnouncePageDTO dto) {
        return Result.success(service.operationPage(dto));
    }

    @GetMapping("/detail")
    public Result<OperationAnnounceDetailVO> detail(@RequestParam("id") Long id) {
        return Result.success(service.operationDetail(id));
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody AnnounceCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody AnnounceUpdateDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("/changeStatus")
    public Result<Void> changeStatus(@Valid @RequestBody AnnounceChangeStatusDTO dto) {
        service.changeStatus(dto.getId(), dto.getStatus());
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdDTO dto) {
        service.delete(dto.getId());
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<AnnounceStatsVO> stats() {
        return Result.success(service.stats());
    }
}
