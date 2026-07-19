package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import org.springframework.stereotype.Component;

/**
 * 把 UAA 登录态解析成<b>本平台的业务身份</b>（本地 sys_user 档案 + 业务角色）。
 *
 * <p><b>为什么业务角色不能用 {@code @PreAuthorize} 查 UAA 角色</b>（实测取证）：
 * UAA 的 {@code /me} 对真实账号返回 100+ 个角色，绝大多数是不透明随机 ID
 * （形如 {@code GJDUnLU6NkAonUmANdV}），少数可读的也是 {@code KPI_ADMIN}、
 * {@code training_manager} 这类<b>全公司级</b>角色。UAA 里<b>根本不存在</b>
 * 本平台 PRD 定义的 {@code sales / product_manager / admin}。
 * 因此 {@code hasAnyRole('sales','admin')} 对任何真实用户都永不放行。
 *
 * <p>本平台的业务角色由运营在本地 {@code sys_user.role} 维护（单人单角色），
 * 这与 CLAUDE.md §3「部门/产品线/用户归属全部运营本地维护」一致。
 *
 * <p><b>关联键是 id 不是 username</b>：schema.sql 明确 {@code sys_user.id = UAA 用户 id}，
 * 且 {@code opportunity_request.publisher_id} 是指向它的 FK，用 id 才能保持引用一致。</p>
 */
@Component
public class CurrentUserResolver {

    public static final String ROLE_SALES = "sales";
    public static final String ROLE_PRODUCT_MANAGER = "product_manager";
    public static final String ROLE_ADMIN = "admin";

    private static final String STATUS_ACTIVE = "active";

    private final SysUserDao sysUserDao;

    public CurrentUserResolver(SysUserDao sysUserDao) {
        this.sysUserDao = sysUserDao;
    }

    /** 当前登录人在本地的档案；未开通则返回 null（调用方决定是拒绝还是降级）。 */
    public SysUserDO currentOrNull() {
        Long uid = SecurityUtils.getCurrentUserId();
        return uid == null ? null : sysUserDao.getById(uid);
    }

    /**
     * 校验当前登录人具备指定业务角色之一，并返回其本地档案。
     *
     * <p>失败一律抛 {@code FORBIDDEN}：<b>fail-closed</b>——查不到本地档案说明该账号
     * 尚未在本平台开通，此时无法判定其业务角色，只能拒绝，不能放行。</p>
     *
     * <p>抛 {@link BaseException} 而非 Spring 的 {@code AccessDeniedException}：
     * 后者不被框架 {@code GlobalExceptionHandler} 识别，会兜成
     * {@code {"code":500,"msg":"系统内部错误"}}，用户看不出是权限问题。</p>
     */
    public SysUserDO requireAnyRole(String... roles) {
        SysUserDO me = currentOrNull();
        if (me == null) {
            throw new BaseException(ErrorCode.FORBIDDEN,
                    "当前账号尚未在本平台开通，请联系运营维护用户档案");
        }
        if (!STATUS_ACTIVE.equals(me.getStatus())) {
            throw new BaseException(ErrorCode.FORBIDDEN, "当前账号已停用");
        }
        for (String r : roles) {
            if (r.equals(me.getRole())) {
                return me;
            }
        }
        throw new BaseException(ErrorCode.FORBIDDEN,
                "当前角色无权执行该操作，需要：" + String.join(" / ", roles));
    }
}
