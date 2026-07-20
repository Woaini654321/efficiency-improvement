package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.DiscussionReplyDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionCommentVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.DiscussionPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 讨论区业务。 */
public interface DiscussionService {

    /** 分页查询：keyword 模糊 title/content，topic 精确过滤；is_hot 倒序→create_time 倒序。 */
    PageVO<DiscussionPageVO> page(DiscussionPageDTO dto);

    /** 详情：view_count 原子自增，并组装递归回帖树。 */
    DiscussionDetailVO detail(Long id);

    /** 发布讨论帖，返回新帖 id。 */
    Long create(DiscussionCreateDTO dto);

    /** 回帖：写 discussion_reply + 同事务 reply_count 原子 +1，返回新回帖节点。 */
    DiscussionCommentVO reply(DiscussionReplyDTO dto);
}
