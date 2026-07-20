package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 升级时间线单行。类级 @JsonNaming 出 snake_case（嵌套 VO 也须各自声明，类级策略不级联）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SlaTimelineVO {

    /** 展示时间，格式 MM-dd HH:mm（纯展示字符串，非 LocalDateTime） */
    private String time;

    /** 事件描述 */
    private String desc;

    /** 通知对象文案（如「发布人 张伟、直属主管」） */
    private String notifyTo;
}
