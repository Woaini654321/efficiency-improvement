package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.DiscussionReplyDO;

/**
 * 讨论回帖 DAO。
 *
 * <p>discussion_reply 是<b>独立单主键表</b>（自身 id 雪花主键 + parent_id 自引用），
 * 不是 opportunity_category 那样的复合主键关联表，故可正常 getById/save/list，
 * 无「禁 removeById」限制。</p>
 */
public interface DiscussionReplyDao extends IService<DiscussionReplyDO> {
}
