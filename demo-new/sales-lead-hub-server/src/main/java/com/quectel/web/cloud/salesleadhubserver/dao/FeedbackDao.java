package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.FeedbackDO;

/** 吐槽墙 DAO。 */
public interface FeedbackDao extends IService<FeedbackDO> {

    /**
     * 点赞数原子自增（{@code SET like_count = like_count + 1 WHERE id = ?}）。
     *
     * <p>SQL 原子自增而非「读出 +1 再写回」：like_count 无乐观锁保护，并发读改写会丢计数。
     * 无去重约束（吐槽墙点赞不记名，简单自增即可）。</p>
     *
     * @return true=命中该行；false=行不存在
     */
    boolean increaseLikeCount(Long id);
}
