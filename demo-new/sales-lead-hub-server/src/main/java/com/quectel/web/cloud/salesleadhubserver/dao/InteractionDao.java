package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.InteractionDO;

/**
 * 互动记录（interaction）DAO：评论(≤2级) / 点赞 / 收藏统一单表。
 *
 * <p><b>点赞/收藏取消一律物理删</b>：按 reaction 维度（user_id + target_type + target_id + type）
 * {@code LambdaQueryWrapper} remove，<b>不要</b> removeById（前端拿不到 reaction 行的雪花 id，
 * 只知道自己在哪个目标上点了赞）。</p>
 *
 * <p>本表<b>无</b>框架 deleted 逻辑删除列；评论软删走普通业务列 content_deleted，同样不经框架。</p>
 */
public interface InteractionDao extends IService<InteractionDO> {
}
