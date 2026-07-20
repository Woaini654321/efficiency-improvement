package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditChangePinDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditChangeStatusDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.IdDTO;
import com.quectel.web.cloud.salesleadhubserver.service.AuditService;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 运营内容审核接口。路径与前端 {@code apis/audit/auditApi.ts} 的 url 字面量逐字对应：
 * {@code audit/page}、{@code audit/changeStatus}、{@code audit/changePin}、{@code audit/delete}。
 *
 * <p>刻意没有 {@code @PreAuthorize}：UAA 无本平台业务角色（理由见 CurrentUserResolver），
 * 业务鉴权由 service 层 {@code requireAnyRole(ROLE_ADMIN)} 查本地 sys_user.role 判定，
 * 非 admin fail-closed 抛 FORBIDDEN。跨 opportunity 与 opportunity_request 两表的联合审核视图。</p>
 */
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<AuditPageVO>> page(@RequestBody AuditPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @PostMapping("/changeStatus")
    public Result<Void> changeStatus(@Valid @RequestBody AuditChangeStatusDTO dto) {
        service.changeStatus(dto.getId(), dto.getStatus());
        return Result.success();
    }

    @PostMapping("/changePin")
    public Result<Void> changePin(@Valid @RequestBody AuditChangePinDTO dto) {
        service.changePin(dto);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdDTO dto) {
        service.delete(dto.getId());
        return Result.success();
    }
}
