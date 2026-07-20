package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.quectel.web.cloud.salesleadhubserver.pojo.Attachment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商机详情出参。与 {@link OpportunityPageVO} 刻意不互相继承——
 * 扁平契约类型，任一侧增减字段编译器能直接指到（理由见 RequirementDetailVO）。
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpportunityDetailVO {

    /** 前端 adapter 读的键名是 opportunity_id（不是 id） */
    private Long opportunityId;

    private String title;

    private String summary;

    /** 富文本 HTML 正文，仅详情下发 */
    private String content;

    /** product_info / solution / success_case */
    private String type;

    /** draft / published / archived */
    private String status;

    private String publisherName;

    private String publisherDeptName;

    private List<String> categoryNames;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private Integer commentCount;

    /** {name,url,size}，字段全小写单词，snake 化是 no-op */
    private List<Attachment> attachments;

    /** 必须带 pattern：默认 ISO 'T' 会让前端 replace(/-/g,'/') 解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /** 编辑页回填后随 update 提交，参与乐观锁 */
    private Integer version;
}
