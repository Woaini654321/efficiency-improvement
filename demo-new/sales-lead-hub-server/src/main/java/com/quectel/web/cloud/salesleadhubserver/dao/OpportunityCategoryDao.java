package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityCategoryDO;

/**
 * 商机-分类关联 DAO。
 *
 * <p>⚠️ 复合主键 (opportunity_id, category_id)：关系维护只能走
 * {@code LambdaQueryWrapper} 按 opportunityId 批量 remove + saveBatch，
 * <b>禁用 getById/removeById</b>（单主键模型只按第一列匹配，语义是错的）。</p>
 */
public interface OpportunityCategoryDao extends IService<OpportunityCategoryDO> {
}
