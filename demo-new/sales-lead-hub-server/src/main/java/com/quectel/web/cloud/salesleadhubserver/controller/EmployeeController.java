package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.EmployeePageDTO;
import com.quectel.web.cloud.salesleadhubserver.service.EmployeeService;
import com.quectel.web.cloud.salesleadhubserver.vo.EmployeePageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人员选择器接口。路径与前端 {@code employee/page} url 字面量逐字一致。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 本端点登录即可查，无角色门槛（供人员选择器使用），故 service 层也不做角色校验。</p>
 */
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<EmployeePageVO>> page(@RequestBody EmployeePageDTO dto) {
        return Result.success(service.page(dto));
    }
}
