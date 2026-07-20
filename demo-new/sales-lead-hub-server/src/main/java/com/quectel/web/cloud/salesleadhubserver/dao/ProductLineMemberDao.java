package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;

/**
 * 产品线成员 DAO。
 *
 * <p>与 opportunity_category 等复合主键关联表不同，product_line_member 有<b>独立主键 id</b>，
 * 故可正常 {@code getById/removeById/updateById}，无需走 LambdaQueryWrapper 全删重写。</p>
 */
public interface ProductLineMemberDao extends IService<ProductLineMemberDO> {
}
