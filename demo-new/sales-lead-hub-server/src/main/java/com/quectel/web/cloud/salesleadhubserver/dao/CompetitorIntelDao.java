package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.CompetitorIntelDO;

/** 竞品情报 DAO。 */
public interface CompetitorIntelDao extends IService<CompetitorIntelDO> {

    /**
     * 浏览数原子自增（{@code SET view_count = view_count + 1 WHERE id = ?}）。
     *
     * <p>SQL 原子自增而非「读出 +1 再写回」：view_count 无乐观锁保护，并发读改写会丢计数。</p>
     *
     * @return true=命中该行；false=行不存在
     */
    boolean increaseViewCount(Long id);
}
