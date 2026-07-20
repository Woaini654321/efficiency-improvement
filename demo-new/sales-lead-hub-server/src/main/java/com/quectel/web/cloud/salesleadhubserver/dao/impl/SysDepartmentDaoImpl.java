package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.SysDepartmentMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysDepartmentDO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SysDepartmentDaoImpl extends ServiceImpl<SysDepartmentMapper, SysDepartmentDO>
        implements SysDepartmentDao {

    @Override
    public List<SysDepartmentDO> listActiveOrderBySort() {
        // WHERE is_active = 1 ORDER BY sort_order ASC
        return this.lambdaQuery()
                .eq(SysDepartmentDO::getIsActive, 1)
                .orderByAsc(SysDepartmentDO::getSortOrder)
                .list();
    }
}
