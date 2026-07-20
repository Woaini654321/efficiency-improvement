package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 商机信息业务。 */
public interface OpportunityService {

    Long create(OpportunityCreateDTO dto);

    void update(OpportunityUpdateDTO dto);

    PageVO<OpportunityPageVO> page(OpportunityPageDTO dto);

    OpportunityDetailVO detail(Long id);

    /** 下架（archived，记下架人）/ 恢复上架（published，谁下架谁恢复）。 */
    void changeStatus(Long id, String status);
}
