package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.service.HomeService;
import com.quectel.web.cloud.salesleadhubserver.vo.HomeDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页工作台接口。路径与前端 {@code apis/home/homeApi.ts} 的 url 字面量逐字一致。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 首页登录即可访问，不校业务角色（个性化段在取不到当前用户时安全降级为空）。</p>
 */
@RestController
@RequestMapping("/home")
public class HomeController {

    private final HomeService service;

    public HomeController(HomeService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Result<HomeDashboardVO> dashboard() {
        return Result.success(service.dashboard());
    }
}
