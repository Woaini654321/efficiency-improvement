package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.service.RequirementService;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementPageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 商机需求接口。路径与前端 {@code apis/requirement/requirementApi.ts} 一一对应。
 *
 * <p><b>这里刻意没有 {@code @PreAuthorize}</b>。登录态由框架
 * {@code TokenValidationFilter} 统一保证；而本平台的业务角色
 * （sales / product_manager / admin）<b>不在 UAA 里</b>——实测 UAA 对真实账号返回
 * 100+ 个不透明随机 ID 形式的全公司级角色，{@code hasAnyRole('sales','admin')}
 * 对任何真实用户都永不放行。业务角色改由 service 层查本地 {@code sys_user.role}
 * 判定，见 {@code CurrentUserResolver}。</p>
 */
@RestController
@RequestMapping("/requirement")
public class RequirementController {

    private final RequirementService service;

    public RequirementController(RequirementService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<RequirementPageVO>> page(@RequestBody RequirementPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @GetMapping("/detail")
    public Result<RequirementDetailVO> detail(@RequestParam("id") Long id) {
        return Result.success(service.detail(id));
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody RequirementCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody RequirementUpdateDTO dto) {
        service.update(dto);
        return Result.success();
    }
}
