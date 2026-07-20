package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.service.ProfileService;
import com.quectel.web.cloud.salesleadhubserver.vo.ProfileCenterVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口。路径与前端 {@code apis/profile/profileApi.ts} 的 url 字面量一致：{@code profile/center}。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无业务角色，理由见 OpportunityController）；
 * 且个人中心对任何登录人开放<b>自己的</b>聚合数据，归属边界由 service 层用当前登录人档案强制，
 * 不接受 userId 入参（防越权查他人）。GET 无请求体。</p>
 */
@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/center")
    public Result<ProfileCenterVO> center() {
        return Result.success(service.center());
    }
}
