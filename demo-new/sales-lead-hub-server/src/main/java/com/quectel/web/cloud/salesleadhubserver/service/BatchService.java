package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.BatchPublishDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.BatchMetaVO;

/** 批量发布业务（batch 模块）。 */
public interface BatchService {

    /** 向导元数据：可选会议 + 在职执行人。 */
    BatchMetaVO meta();

    /** 批量发布：复用/新建会议 + 逐条插任务，同一事务。 */
    void publish(BatchPublishDTO dto);
}
