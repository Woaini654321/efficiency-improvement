package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionPostDO;

/** 讨论帖主表 DAO。 */
public interface DiscussionPostDao extends IService<DiscussionPostDO> {

    /**
     * 浏览数原子自增（{@code SET view_count = view_count + 1}）。
     *
     * <p>用 SQL 原子自增而非「读出 +1 再写回」：view_count 列没有乐观锁保护，
     * 并发下读改写会丢计数。封装成 Dao 方法而非在 service 里裸写 lambdaUpdate，
     * 是为了让 service 单测能直接 mock 它、无需 mock MP 的链式调用。</p>
     *
     * @param id 帖 id
     * @return 是否命中（帖存在则 true）
     */
    boolean increaseViewCount(Long id);

    /**
     * 回帖数原子自增（{@code SET reply_count = reply_count + 1}）。
     *
     * <p>与 {@link #increaseViewCount(Long)} 同理：reply_count 无乐观锁保护，用 SQL
     * 原子自增避免「读出 +1 再写回」在并发下丢计数；封装成 Dao 方法便于 service 单测 mock。
     * 由回帖端点在写入 discussion_reply 的<b>同一事务</b>内调用。</p>
     *
     * @param id 帖 id
     * @return 是否命中（帖存在则 true）
     */
    boolean increaseReplyCount(Long id);
}
