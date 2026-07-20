package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 运营看板·趋势序列组（四种粒度各一条序列，前端按选中 range 取用对应键）。
 *
 * <p>值用 {@link Number}：发布数序列装 Integer、响应率序列装 Double，均属前端 number[] 契约。
 * 字段名 last7d/last4w/last12w/last6m 无驼峰，snake_case 后不变。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardTrendMapVO {

    private List<Number> last7d;

    private List<Number> last4w;

    private List<Number> last12w;

    private List<Number> last6m;
}
