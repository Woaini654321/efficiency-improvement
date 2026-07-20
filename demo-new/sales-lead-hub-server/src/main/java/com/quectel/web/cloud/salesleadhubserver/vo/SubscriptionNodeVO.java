package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 订阅分类树节点。对齐前端 {@code SubscriptionNodeDTO}：title/value/key/children。
 *
 * <p>title=分类名；value=key=分类 id 字符串（前端仅用于 tree-select 勾选态与 subscribed_keys
 * 内部匹配，值本身不展示，故用 DB 主键即可，无需 mock 里的语义 slug）。children 为空时留 null，
 * 全局 NON_NULL 会省略该键，前端 {@code n.children && n.children.length} 兜底。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SubscriptionNodeVO {

    private String title;

    private String value;

    private String key;

    private List<SubscriptionNodeVO> children;
}
