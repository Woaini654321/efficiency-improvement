package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;

/** 审计日志 DAO（当前仅查询；写入走后续 AOP 审计切面）。 */
public interface AuditLogDao extends IService<AuditLogDO> {
}
