package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;

/** 分类字典数据访问。需求侧只读，用于生成 categoryNames 快照。 */
public interface CategoryDao extends IService<CategoryDO> {
}
