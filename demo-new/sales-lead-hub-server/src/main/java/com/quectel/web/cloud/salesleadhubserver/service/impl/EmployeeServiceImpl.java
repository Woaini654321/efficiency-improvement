package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.web.cloud.salesleadhubserver.convert.EmployeeConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.EmployeePageDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.EmployeeService;
import com.quectel.web.cloud.salesleadhubserver.vo.EmployeePageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 人员选择器业务实现。
 *
 * <p>登录即可查（无角色门槛，供可见性配置/任务指派的人员选择器使用）；只返回 status='active'
 * 的用户；keyword 同时模糊匹配姓名 name 与工号 employee_id。出参经 EmployeeConvert 只暴露
 * id/name/employee_id/department_name，敏感字段不下发。</p>
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final String STATUS_ACTIVE = "active";

    private final SysUserDao sysUserDao;
    private final EmployeeConvert convert;

    public EmployeeServiceImpl(SysUserDao sysUserDao, EmployeeConvert convert) {
        this.sysUserDao = sysUserDao;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<EmployeePageVO> page(EmployeePageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 20 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        String kw = dto.getKeyword();
        boolean hasKw = kw != null && !kw.isEmpty();

        Page<SysUserDO> p = new Page<>(pageNumber, pageSize);
        IPage<SysUserDO> r = sysUserDao.lambdaQuery()
                .eq(SysUserDO::getStatus, STATUS_ACTIVE)
                // keyword 同时匹配姓名或工号
                .and(hasKw, w -> w.like(SysUserDO::getName, kw)
                        .or().like(SysUserDO::getEmployeeId, kw))
                .orderByAsc(SysUserDO::getEmployeeId)
                .page(p);

        List<EmployeePageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }
}
