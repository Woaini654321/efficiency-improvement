package com.quectel.web.cloud.salesleadhubserver.dto;

import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页入参基类校验边界。
 *
 * <p>与前端 {@code @q-mono-x/types/base} 的 PaginationParams 契约对齐：
 * 恰好 pageNumber/pageSize/orderBy/orderDirection 四个字段。
 * 默认值由 service 层兜底（null→1/10），DTO 不设默认——保持与
 * RequirementServiceImpl 现行行为一致。</p>
 */
class BasePageDTOTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejects_pageSize_over_framework_maxLimit() {
        BasePageDTO d = new BasePageDTO();
        d.setPageSize(501);   // 框架 maxLimit=500：超出不报错而是静默截断，必须在入口拦下
        assertFalse(validator.validate(d).isEmpty(),
                "pageSize>500 必须被校验拦下，不能留给框架静默截断");
    }

    @Test
    void rejects_non_positive_page() {
        BasePageDTO d = new BasePageDTO();
        d.setPageNumber(0);
        assertFalse(validator.validate(d).isEmpty(), "pageNumber<1 必须被拦下");
    }

    @Test
    void accepts_nulls_service_layer_fills_defaults() {
        BasePageDTO d = new BasePageDTO();   // 四字段全空
        assertTrue(validator.validate(d).isEmpty(),
                "分页字段允许为空，由 service 层兜底默认值");
    }

    @Test
    void requirement_page_dto_inherits_base_constraints() {
        RequirementPageDTO d = new RequirementPageDTO();
        d.setPageSize(501);
        assertFalse(validator.validate(d).isEmpty(),
                "子类必须继承基类的 pageSize 上限校验");
    }
}
