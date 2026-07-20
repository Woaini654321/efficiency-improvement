package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 吐槽项出参。类级 @JsonNaming 出 snake_case（禁全局策略，理由见 RequirementPageVO）。
 *
 * <p><b>刻意不含 create_by/createBy</b>：feedback 表的 create_by 存真实作者仅供后台反滥用，
 * 匿名是本模块产品红线，任何出参都不得回泄真实身份。本类不继承 BaseEntity、不声明该字段，
 * 从结构上保证 create_by 无法被序列化出去。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FeedbackVO {

    /** 前端 adapter 读的键名是 feedback_id（不是 id） */
    private Long feedbackId;

    private String title;

    private String content;

    /** 后端生成的匿名昵称 */
    private String anonName;

    private Integer likeCount;

    private String emoji;

    private String color;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}
