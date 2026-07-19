package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;

/**
 * 本地用户表数据访问。
 *
 * <p>UAA 登录态只给 7 字段、<b>不含部门</b>，故部门/工号等一律查本地 sys_user。
 * 本地 sys_user 由运营维护，其 id 与 UAA id 不保证一致，<b>关联键统一用 username</b>。</p>
 */
public interface SysUserDao extends IService<SysUserDO> {
}
