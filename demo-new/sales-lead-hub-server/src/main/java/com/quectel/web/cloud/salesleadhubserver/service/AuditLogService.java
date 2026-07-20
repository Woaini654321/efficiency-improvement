package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.AuditLogPageDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditLogPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 审计日志查询（仅 admin）。 */
public interface AuditLogService {

    PageVO<AuditLogPageVO> page(AuditLogPageDTO dto);
}
