package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.vo.CategoryVO;
import org.springframework.stereotype.Component;

/** 手写 DTO/VO ⇄ DO 映射；Boolean（契约）↔ Integer（TINYINT 列）转换的唯一桥接点。 */
@Component
public class CategoryConvert {

    public CategoryDO toCreateDO(CategoryCreateDTO dto) {
        CategoryDO d = new CategoryDO();
        d.setName(dto.getName());
        d.setNameEn(dto.getNameEn());
        d.setParentId(parseNullableId(dto.getParentId()));
        d.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        d.setIsActive(Boolean.TRUE.equals(dto.getIsActive()) ? 1 : 0);
        return d;
    }

    public void applyUpdate(CategoryUpdateDTO dto, CategoryDO d) {
        d.setName(dto.getName());
        d.setNameEn(dto.getNameEn());
        d.setParentId(parseNullableId(dto.getParentId()));
        if (dto.getSortOrder() != null) {
            d.setSortOrder(dto.getSortOrder());
        }
        d.setIsActive(Boolean.TRUE.equals(dto.getIsActive()) ? 1 : 0);
    }

    public CategoryVO toVO(CategoryDO d, int contentCount) {
        CategoryVO v = new CategoryVO();
        v.setCategoryId(d.getId());
        v.setName(d.getName());
        v.setNameEn(d.getNameEn());
        v.setParentId(d.getParentId());
        v.setSortOrder(d.getSortOrder());
        v.setIsActive(d.getIsActive() != null && d.getIsActive() == 1);
        v.setContentCount(contentCount);
        return v;
    }

    /** 空串/null=根节点；非数字直接判非法（静默丢会让层级悄悄错位）。 */
    private Long parseNullableId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "父分类 id 非法：" + raw);
        }
    }
}
