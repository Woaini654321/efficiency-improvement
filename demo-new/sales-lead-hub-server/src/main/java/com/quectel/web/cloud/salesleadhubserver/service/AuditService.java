package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.AuditChangePinDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditPageDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/**
 * 运营内容审核服务：跨 opportunity 与 opportunity_request 两表的统一审核视图 + 强制上下架、
 * 置顶、删除。全部方法仅 admin 可调（在 impl 开头 requireAnyRole(ROLE_ADMIN)）。
 */
public interface AuditService {

    /** 分页审核视图：按 contentType 分流单表，或不传时合并两表。 */
    PageVO<AuditPageVO> page(AuditPageDTO dto);

    /** 运营强制上/下架：按目标 status 分流到对应表。 */
    void changeStatus(Long id, String status);

    /** 置顶/取消置顶（可选一并写 sortNo）：按 id 探两表分流。 */
    void changePin(AuditChangePinDTO dto);

    /** 删除（框架逻辑删 deleted='Y'）：按 id 探两表分流。 */
    void delete(Long id);
}
