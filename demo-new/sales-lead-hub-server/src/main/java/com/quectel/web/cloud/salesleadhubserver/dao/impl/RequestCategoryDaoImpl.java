package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.RequestCategoryMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;
import org.springframework.stereotype.Repository;

@Repository
public class RequestCategoryDaoImpl extends ServiceImpl<RequestCategoryMapper, RequestCategoryDO>
        implements RequestCategoryDao {
}
