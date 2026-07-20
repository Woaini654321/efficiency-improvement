package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 催办元数据·邮件通知人候选。label 展示、value 为邮箱。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SlaEmailContactVO {

    /** 展示文案，如「李明 Ming.Li — ming.li@company.com」 */
    private String label;

    /** 邮箱地址 */
    private String value;
}
