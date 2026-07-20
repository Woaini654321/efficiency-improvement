package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 当前登录人已订阅的分类 key 集合（分类 id 字符串），与 {@link SubscriptionTreeVO} 的 value/key 对齐。
 *
 * <p>同 SubscriptionTreeVO 的双域说明：category 无域判别列，全部已订阅 key 归入 opportunity、
 * requirement 置空。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SubscribedKeysVO {

    private List<String> opportunity;

    private List<String> requirement;
}
