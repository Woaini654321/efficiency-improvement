package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.FeedbackCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.FeedbackVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 吐槽墙业务。 */
public interface FeedbackService {

    /** 列表：按 create_time 倒序，上限 500 行，登录即可。 */
    PageVO<FeedbackVO> list();

    /** 发布匿名吐槽：anon_name/emoji/color 由后端生成，返回新行 id。 */
    Long create(FeedbackCreateDTO dto);

    /** 点赞：like_count 原子自增（无去重）。 */
    void like(Long id);
}
