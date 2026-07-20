package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.MeetingCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.MeetingTaskPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 会议任务管理业务（meeting 模块）。 */
public interface MeetingService {

    PageVO<MeetingTaskPageVO> page(MeetingPageDTO dto);

    /** 新建：按 (会议名,会议时间) 复用/新建会议，再插一条任务。返回任务 id。 */
    Long create(MeetingCreateDTO dto);

    /** 编辑任务行（含会议快照列）。 */
    void update(MeetingUpdateDTO dto);

    /** 催办：更新最近催办时间（通知推送后续接入）。 */
    void urge(Long id, String remark);

    /** 作废：未完成的任务可作废，记原因。 */
    void cancel(Long id, String reason);
}
