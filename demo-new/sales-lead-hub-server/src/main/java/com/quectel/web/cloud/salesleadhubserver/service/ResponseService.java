package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.RequirementAdoptDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCloseDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.ResponseCreateDTO;

/**
 * 需求-方案匹配闭环：提交方案 / 采纳 / 关闭。
 *
 * <p>三者都要跨表同事务改动（solution_response + opportunity_request + notification），
 * 故聚在一个 service，避免闭环逻辑散落到 RequirementService 与调用方拼装。</p>
 */
public interface ResponseService {

    /** 提交方案：落 solution_response + 需求计数/停表 + 通知发布人。@return 新方案 id */
    Long create(ResponseCreateDTO dto);

    /** 采纳方案：需求与方案双写（单一真相源）+ 通知响应人。 */
    void adopt(RequirementAdoptDTO dto);

    /** 关闭需求：Pending/Collecting → Closed。 */
    void close(RequirementCloseDTO dto);
}
