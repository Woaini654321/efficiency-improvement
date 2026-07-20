package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * 催办元数据出参：产品线负责人候选 + 邮件通知人候选。嵌套 VO 各自声明 @JsonNaming（类级策略不级联）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SlaMetaVO {

    private List<SlaProductLeadVO> productLeads;

    private List<SlaEmailContactVO> emailContacts;
}
