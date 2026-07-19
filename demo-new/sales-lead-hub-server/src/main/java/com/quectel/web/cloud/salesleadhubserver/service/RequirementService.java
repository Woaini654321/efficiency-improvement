package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementPageVO;

/** 商机需求核心 CRUD。 */
public interface RequirementService {

    /** @return 新建需求的雪花 id */
    Long create(RequirementCreateDTO dto);

    void update(RequirementUpdateDTO dto);

    PageVO<RequirementPageVO> page(RequirementPageDTO dto);

    RequirementDetailVO detail(Long id);
}
