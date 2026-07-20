package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 分页入参基类，各模块 PageDTO 继承（子类记得 {@code @EqualsAndHashCode(callSuper = true)}）。
 *
 * <p>字段与前端 {@code @q-mono-x/types/base} 的 PaginationParams 契约逐字对齐：
 * 恰好 pageNumber/pageSize/orderBy/orderDirection 四个，keyword 等属模块扩展，
 * 由子类自行声明。</p>
 *
 * <p>DTO 保持 camelCase：全局 Jackson 未开 snake_case（也绝不能开——双向策略会让
 * 前端 camelCase 入参静默变 null），出参 snake_case 用 VO 类级 @JsonNaming。</p>
 *
 * <p>不设默认值：null 的兜底（1/10）在 service 层做，与 RequirementServiceImpl
 * 现行行为保持一致。</p>
 */
@Data
public class BasePageDTO {

    /** 页码，从 1 开始 */
    @Min(1)
    private Integer pageNumber;

    /**
     * 每页条数。上限 500——框架 PaginationInnerInterceptor 的 maxLimit=500，
     * 超出不报错而是静默截断（返 500 条但 total 是全量），此处在入口显式拦下。
     */
    @Min(1)
    @Max(500)
    private Integer pageSize;

    /** 排序字段，可空 */
    private String orderBy;

    /** 排序方向 asc/desc，可空 */
    private String orderDirection;
}
