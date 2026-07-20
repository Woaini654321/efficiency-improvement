package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineDO;
import com.quectel.web.cloud.salesleadhubserver.service.ProductLineOptionService;
import com.quectel.web.cloud.salesleadhubserver.vo.ProductLineOptionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品线选项业务实现。
 *
 * <p>登录即可查（无角色门槛，与 Employee 人员选择器同为只读选择器，供需求发布表单的
 * 「邀请产品线」使用；若加 admin 门槛，普通销售发需求时选择器会空/403）。已登录由 SSO
 * TokenValidationFilter 保证，故 service 层不再做角色校验。</p>
 *
 * <p>只取 is_active=1 的产品线（过滤在 {@code ProductLineDao.listActiveOrderByName} 内的
 * WHERE 子句），映射为仅含 id/name 的选项 VO，内联组装（端点简单，不单列 Convert）。</p>
 */
@Service
public class ProductLineOptionServiceImpl implements ProductLineOptionService {

    private final ProductLineDao productLineDao;

    public ProductLineOptionServiceImpl(ProductLineDao productLineDao) {
        this.productLineDao = productLineDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductLineOptionVO> list() {
        return productLineDao.listActiveOrderByName().stream()
                .map(ProductLineOptionServiceImpl::toOptionVO)
                .collect(Collectors.toList());
    }

    private static ProductLineOptionVO toOptionVO(ProductLineDO d) {
        ProductLineOptionVO v = new ProductLineOptionVO();
        v.setProductLineId(d.getId());
        v.setName(d.getName());
        return v;
    }
}
