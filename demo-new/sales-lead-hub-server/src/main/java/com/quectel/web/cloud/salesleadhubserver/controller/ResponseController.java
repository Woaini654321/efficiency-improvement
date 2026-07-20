package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.ResponseCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.service.ResponseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 方案响应接口。路径与前端 {@code response/create} 逐字对齐。
 *
 * <p>不加 {@code @PreAuthorize}：业务角色不在 UAA，由 service 层查本地 sys_user 判定，
 * 理由见 {@code RequirementController} / {@code CurrentUserResolver}。</p>
 */
@RestController
@RequestMapping("/response")
public class ResponseController {

    private final ResponseService service;

    public ResponseController(ResponseService service) {
        this.service = service;
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody ResponseCreateDTO dto) {
        return Result.success(service.create(dto));
    }
}
