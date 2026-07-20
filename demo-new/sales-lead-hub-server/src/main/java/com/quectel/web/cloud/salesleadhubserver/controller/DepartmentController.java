package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.service.DepartmentService;
import com.quectel.web.cloud.salesleadhubserver.vo.DeptNodeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门树接口。路径与前端 {@code department/tree} url 字面量逐字一致。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 本端点登录即可查、无角色门槛（供需求可见性的部门选择器使用），故 service 层也不做角色校验，
 * 已登录由 SSO TokenValidationFilter 保证。</p>
 */
@RestController
@RequestMapping("/department")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping("/tree")
    public Result<List<DeptNodeVO>> tree() {
        return Result.success(service.tree());
    }
}
