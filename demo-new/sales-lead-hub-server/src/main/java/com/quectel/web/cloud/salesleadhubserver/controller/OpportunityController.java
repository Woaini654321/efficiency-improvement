package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityChangeStatusDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.service.OpportunityService;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 商机信息接口。路径与前端 {@code apis/opportunity/opportunityApi.ts} 一一对应。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无业务角色，理由见 RequirementController）；
 * 业务角色由 service 层 {@code CurrentUserResolver} 查本地 sys_user.role 判定。</p>
 *
 * <p>前端 api 还导出了 {@code opportunity/delete}，但无任何页面调用（死代码），
 * 按 YAGNI 不实现；若运营侧后续需要删除，走 audit 模块的运营端点。</p>
 */
@RestController
@RequestMapping("/opportunity")
public class OpportunityController {

    private final OpportunityService service;

    public OpportunityController(OpportunityService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<OpportunityPageVO>> page(@RequestBody OpportunityPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @GetMapping("/detail")
    public Result<OpportunityDetailVO> detail(@RequestParam("id") Long id) {
        return Result.success(service.detail(id));
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody OpportunityCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody OpportunityUpdateDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("/changeStatus")
    public Result<Void> changeStatus(@Valid @RequestBody OpportunityChangeStatusDTO dto) {
        service.changeStatus(dto.getId(), dto.getStatus());
        return Result.success();
    }
}
