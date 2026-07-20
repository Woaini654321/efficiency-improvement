package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运营内容审核列表项出参（跨 opportunity + opportunity_request 的统一视图）。
 *
 * <p>类级 @JsonNaming 出 snake_case（禁全局策略，理由见 RequirementPageVO）。字段以前端
 * {@code apis/audit/auditAdapter.ts} 读取的键名为准：audit_id/content_type/publisher_name/
 * is_pinned/published_at/sort_no 等。前端 adapter 对每个键都有 {@code ??} 默认值兜底，
 * 故两表缺失的键（如商机无 urgency、需求无 tags）下发 null 安全落默认。</p>
 *
 * <p>{@code content_type} 是本 VO 的核心标识：opportunity=商机 / request=需求，前端据此
 * 分 tab 展示并决定详情/编辑跳转路由。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuditPageVO {

    /** 前端 adapter 读的键名是 audit_id（= 各源表雪花主键，两表全局唯一） */
    private Long auditId;

    private String title;

    /** opportunity=商机 / request=需求 */
    private String contentType;

    private String publisherName;

    /** opportunity: published/archived；request: Pending/Collecting/Adopted/Closed */
    private String status;

    private Boolean isPinned;

    /** 发布时间（取源表 create_time）。必须带 pattern：默认 ISO 'T' 会让前端解析出 NaN */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedAt;

    private Integer sortNo;

    /** normal/urgent/critical。商机表无该列，统一下发 normal */
    private String urgency;

    /** 行业。商机表无该列，下发 null */
    private String industry;

    /** 标签。两表均无独立 tags 列，下发 null（前端 adapter ?? [] 兜底） */
    private List<String> tags;

    /** 级联分类路径 string[][]。与两表的 category_names(List&lt;String&gt;) 形状不同，下发 null */
    private List<List<String>> categoryPath;

    /** 描述。列表页不渲染且需求 description 为 MEDIUMTEXT，全量拉取时下发 null 以免拖带宽 */
    private String description;
}
