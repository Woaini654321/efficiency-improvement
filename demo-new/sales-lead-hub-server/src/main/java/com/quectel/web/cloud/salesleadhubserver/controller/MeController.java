package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.security.entity.User;
import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.code.web.pojo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录用户信息 —— SSO(UAA) 骨架示例接口。
 * 需携带有效 UAA token 访问；未认证时框架返回 401。
 */
@RestController
@RequestMapping("/me")
public class MeController {

    /** 获取当前登录用户完整信息 */
    @GetMapping
    public Result<User> currentUser() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new BaseException(ErrorCode.UNAUTHORIZED);
        }
        return Result.success(user);
    }

    /** 仅获取当前登录用户 ID */
    @GetMapping("/id")
    public Result<Long> currentUserId() {
        return Result.success(SecurityUtils.getCurrentUserId());
    }
}
