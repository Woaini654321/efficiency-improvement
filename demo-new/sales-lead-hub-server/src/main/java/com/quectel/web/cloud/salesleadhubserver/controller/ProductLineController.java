package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.service.ProductLineOptionService;
import com.quectel.web.cloud.salesleadhubserver.vo.ProductLineOptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产品线选项接口。路径与前端 {@code productLine/list} url 字面量逐字一致。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 本端点登录即可查、无角色门槛（供需求发布表单的产品线选择器使用，不能设 admin 门槛，
 * 否则普通销售发需求时选不到产品线），故 service 层也不做角色校验，已登录由 SSO 保证。</p>
 */
@RestController
@RequestMapping("/productLine")
public class ProductLineController {

    private final ProductLineOptionService service;

    public ProductLineController(ProductLineOptionService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public Result<List<ProductLineOptionVO>> list() {
        return Result.success(service.list());
    }
}
