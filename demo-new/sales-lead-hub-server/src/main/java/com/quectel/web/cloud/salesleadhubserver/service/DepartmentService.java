package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.vo.DeptNodeVO;

import java.util.List;

/** 部门树业务（只读，查启用中的 sys_department 组树）。 */
public interface DepartmentService {

    /** 启用中的部门树，按 parent_id 组树、同级按 sort_order 升序。 */
    List<DeptNodeVO> tree();
}
