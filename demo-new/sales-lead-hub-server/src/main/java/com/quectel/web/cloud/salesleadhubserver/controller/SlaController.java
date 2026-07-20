package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.SlaPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.SlaUrgeDTO;
import com.quectel.web.cloud.salesleadhubserver.service.SlaService;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaMetaVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaRequestVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaStatsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 需求时效（SLA）监控接口。路径与前端 {@code apis/sla/slaApi.ts} 的 url 字面量逐字一致
 * （注意 {@code operation/} 前缀）。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 全部端点仅 admin，由 service 层 {@code requireAnyRole(ROLE_ADMIN)} fail-closed 判定。</p>
 *
 * <p>{@code /page} 不加 {@code @Valid}：前端列表页以 {@code pageSize:999} 拉全量本地分页，
 * 加 @Valid 会因 {@code @Max(500)} 直接 400；service 内部已夹逼到 maxLimit。</p>
 */
@RestController
@RequestMapping("/operation/sla")
public class SlaController {

    private final SlaService service;

    public SlaController(SlaService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<SlaRequestVO>> page(@RequestBody SlaPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @GetMapping("/stats")
    public Result<SlaStatsVO> stats() {
        return Result.success(service.stats());
    }

    @GetMapping("/meta")
    public Result<SlaMetaVO> meta() {
        return Result.success(service.meta());
    }

    @PostMapping("/urge")
    public Result<Void> urge(@Valid @RequestBody SlaUrgeDTO dto) {
        service.urge(dto);
        return Result.success();
    }
}
