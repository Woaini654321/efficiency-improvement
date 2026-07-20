package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.DashboardQueryDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.DashboardVO;

/**
 * 运营数据看板聚合。仅运营（admin）可用；对既有表只读聚合，不建新表。
 * 环比与上一等长周期比较（首次上线无历史时环比为 0，不造假）；所有除法防 0。
 */
public interface DashboardService {

    DashboardVO dashboard(DashboardQueryDTO dto);
}
