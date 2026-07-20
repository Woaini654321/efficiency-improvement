package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * 分类列表项出参（扁平，前端自建树）。
 *
 * <p>{@code parentId} 为 null（根节点）时，框架全局 NON_NULL 会把整个键省略，
 * 前端 adapter 的 {@code dto.parent_id ?? ''} 兜底——契约测试钉死该行为。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CategoryVO {

    /** 前端 adapter 读的键名是 category_id */
    private Long categoryId;

    private String name;

    private String nameEn;

    private Long parentId;

    private Integer sortOrder;

    /** DB 是 TINYINT(1)，契约是 boolean，convert 负责转换 */
    private Boolean isActive;

    /** 引用该分类的内容数（商机+需求关联行聚合），删除防呆的展示依据 */
    private Integer contentCount;
}
