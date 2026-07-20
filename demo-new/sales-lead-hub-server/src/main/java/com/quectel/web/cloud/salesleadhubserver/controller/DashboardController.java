package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.DashboardQueryDTO;
import com.quectel.web.cloud.salesleadhubserver.service.DashboardService;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运营数据看板接口。路径与前端 {@code apis/dashboard/dashboardApi.ts} 的 url 字面量逐字一致
 * （注意 {@code operation/} 前缀，且无 {@code /} 结尾子路径——就是 {@code operation/dashboard}）。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 仅 admin，由 service 层 {@code requireAnyRole(ROLE_ADMIN)} fail-closed 判定。</p>
 *
 * <p>入参 {@code {range?}} 允许为空（默认 last7d），故 body 可缺省，不加 {@code @Valid}。</p>
 */
@RestController
@RequestMapping("/operation")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @PostMapping("/dashboard")
    public Result<DashboardVO> dashboard(@RequestBody(required = false) DashboardQueryDTO dto) {
        return Result.success(service.dashboard(dto));
    }
}
