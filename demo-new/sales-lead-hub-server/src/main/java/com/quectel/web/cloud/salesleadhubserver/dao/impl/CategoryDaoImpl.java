package com.quectel.web.cloud.salesleadhubserver.dao.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.mapper.CategoryMapper;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryDaoImpl extends ServiceImpl<CategoryMapper, CategoryDO>
        implements CategoryDao {
}
