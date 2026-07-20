package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.vo.EmployeePageVO;
import org.springframework.stereotype.Component;

/**
 * 手写 SysUserDO → EmployeePageVO 映射（不引 MapStruct）。
 *
 * <p><b>只搬 4 个非敏感字段</b>：id/name/employee_id/department_name。phone/email/avatar 等
 * 敏感字段刻意不映射——从源头杜绝人员选择器泄露敏感信息。</p>
 */
@Component
public class EmployeeConvert {

    public EmployeePageVO toPageVO(SysUserDO u) {
        EmployeePageVO v = new EmployeePageVO();
        v.setId(u.getId());
        v.setName(u.getName());
        v.setEmployeeId(u.getEmployeeId());
        v.setDepartmentName(u.getDepartmentName());
        return v;
    }
}
