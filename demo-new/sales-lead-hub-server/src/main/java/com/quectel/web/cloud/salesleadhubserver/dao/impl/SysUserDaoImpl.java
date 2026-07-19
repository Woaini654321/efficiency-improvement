package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.SysUserMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import org.springframework.stereotype.Repository;

@Repository
public class SysUserDaoImpl extends ServiceImpl<SysUserMapper, SysUserDO>
        implements SysUserDao {
}
