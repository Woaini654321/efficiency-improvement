package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.vo.ProductLineOptionVO;

import java.util.List;

/** 产品线选项业务（只读，查启用中的 product_line）。 */
public interface ProductLineOptionService {

    /** 启用中的产品线选项，按 name 升序。 */
    List<ProductLineOptionVO> list();
}
