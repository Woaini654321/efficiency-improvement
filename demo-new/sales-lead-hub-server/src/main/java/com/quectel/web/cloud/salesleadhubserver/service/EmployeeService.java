package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.EmployeePageDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.EmployeePageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 人员选择器业务（只读，查本地 sys_user）。 */
public interface EmployeeService {

    PageVO<EmployeePageVO> page(EmployeePageDTO dto);
}
