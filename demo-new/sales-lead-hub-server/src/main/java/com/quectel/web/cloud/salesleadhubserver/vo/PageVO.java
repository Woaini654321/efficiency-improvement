package com.quectel.web.cloud.salesleadhubserver.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 统一分页出参：records + total，与前端 {@code PaginationResult} 对齐。
 *
 * <p>框架无统一分页返回类型（反编译取证：mysql-starter 只注册
 * {@code PaginationInnerInterceptor}，不提供出参包装），故本模块自定义。</p>
 */
@Data
@AllArgsConstructor
public class PageVO<T> {

    private List<T> records;

    private long total;
}
