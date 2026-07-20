package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分类列表入参。前端会发 dictType，DB 无对应列——刻意不声明，Jackson 静默忽略。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CategoryListDTO extends BasePageDTO {

    /** 名称模糊搜索，可空 */
    private String keyword;
}
