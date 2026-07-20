package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelSubmitDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.IndustryIntelPageDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 情报中心业务（竞品 + 行业）。 */
public interface IntelService {

    /** 竞品分页：keyword 模糊 title/summary；brand/intelType 精确；create_time 倒序。 */
    PageVO<CompetitorIntelPageVO> competitorPage(CompetitorIntelPageDTO dto);

    /** 竞品详情：view_count 原子自增后返回；不存在抛 NOT_FOUND。 */
    CompetitorIntelDetailVO competitorDetail(Long id);

    /** 行业分页：keyword 模糊 title/summary；industry 精确；create_time 倒序。 */
    PageVO<IndustryIntelPageVO> industryPage(IndustryIntelPageDTO dto);

    /** 行业详情：view_count 原子自增后返回；不存在抛 NOT_FOUND。 */
    IndustryIntelDetailVO industryDetail(Long id);

    /** 提交竞品情报：submitter 快照取自当前登录人本地 sys_user，返回新行 id。 */
    Long submitCompetitor(CompetitorIntelSubmitDTO dto);
}
