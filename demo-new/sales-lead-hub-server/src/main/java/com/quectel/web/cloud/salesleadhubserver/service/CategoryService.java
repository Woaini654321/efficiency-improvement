package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.CategoryCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryListDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.CategoryVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 分类维护业务（运营后台）。 */
public interface CategoryService {

    PageVO<CategoryVO> list(CategoryListDTO dto);

    Long create(CategoryCreateDTO dto);

    void update(CategoryUpdateDTO dto);

    /** 删除防呆：有子分类或被内容引用则拒绝。 */
    void delete(Long id);

    void changeActive(Long id, boolean active);
}
