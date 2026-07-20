package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 订阅分类树，按业务域分 opportunity / requirement 两棵。
 *
 * <p>⚠️ schema 的 category 表<b>无「商机域/需求域」判别列</b>（只有 parent_id 自引用树），
 * 无法忠实还原 mock 的双域切分。当前把全部启用分类组成的一棵森林放在 opportunity 下、
 * requirement 置空。若产品要区分双域，需在 category 加 domain 判别列（PRD/SSOT 层面）。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SubscriptionTreeVO {

    private List<SubscriptionNodeVO> opportunity;

    private List<SubscriptionNodeVO> requirement;
}
