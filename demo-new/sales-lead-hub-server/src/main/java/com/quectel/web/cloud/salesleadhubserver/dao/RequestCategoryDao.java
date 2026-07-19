package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;

/**
 * 需求-分类关联表数据访问。
 *
 * <p><b>⚠️ 该表是复合主键</b>，{@code requestId} 上标 {@code IdType.INPUT} 仅为让 insert 工作，
 * 并非真正的单一主键。关系维护必须走 {@code LambdaQueryWrapper} 按 requestId 批量
 * remove + saveBatch，<b>禁用 getById/removeById</b>（它们只按单列匹配，语义是错的）。</p>
 */
public interface RequestCategoryDao extends IService<RequestCategoryDO> {
}
