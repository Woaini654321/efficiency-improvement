package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditLogPageDTO;
import com.quectel.web.cloud.salesleadhubserver.service.AuditLogService;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditLogPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 审计日志接口（只读，仅 admin，路径对齐 {@code apis/auditLog/auditLogApi.ts}）。
 * 审计行的<b>写入</b>由后续 AOP 切面完成（CLAUDE.md §3：自建审计表 + AOP），
 * 当前各业务操作尚未落审计行，接口先行、数据随切面补齐。
 */
@RestController
@RequestMapping("/auditLog")
public class AuditLogController {

    private final AuditLogService service;

    public AuditLogController(AuditLogService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<AuditLogPageVO>> page(@RequestBody AuditLogPageDTO dto) {
        return Result.success(service.page(dto));
    }
}
