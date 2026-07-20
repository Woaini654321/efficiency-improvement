package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.TaskPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.TaskTransferDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.TaskPageVO;

/** 「我的任务」业务（task 模块）。 */
public interface TaskService {

    /** 我的任务分页：仅当前用户为执行人的任务（按 id 或姓名匹配）。 */
    PageVO<TaskPageVO> page(TaskPageDTO dto);

    /** 开始处理：仅 pending → processing。 */
    void start(Long id);

    /** 标记完成：pending/processing → completed，记完成备注。 */
    void complete(Long id, String remark);

    /** 转交：→ transferred，追加转交历史并把执行人换成目标人。 */
    void transfer(TaskTransferDTO dto);
}
