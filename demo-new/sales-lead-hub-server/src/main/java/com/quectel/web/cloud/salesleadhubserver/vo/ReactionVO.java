package com.quectel.web.cloud.salesleadhubserver.vo;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 点赞/收藏切换后的当前状态出参。
 *
 * <p>前端 {@code likeComment} 返回 void、不读该 body；此 VO 主要供离线断言与将来接入使用：
 * {@code liked} = 切换后该用户是否处于已反应状态；{@code likeCount} = 该目标当前反应计数。</p>
 */
@Data
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReactionVO {

    /** 切换后当前用户是否已反应（true=刚点上；false=刚取消） */
    private boolean liked;

    /**
     * 目标当前反应计数（对应 type 的行数）。
     *
     * <p>用 {@link Integer} 而非 {@code long}：计数量级远不及 int 上限，用包装类型与其余
     * 计数字段（view/comment/collect 均为 Integer）保持一致，避免前端因基础类型差异出现
     * 序列化歧义（框架 Long→String 只作用于 Long，不碰基础 long/Integer）。</p>
     */
    private Integer likeCount;
}
