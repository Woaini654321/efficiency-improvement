package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.vo.HomeDashboardVO;

/**
 * 首页工作台聚合。登录即可访问（不校业务角色），返回统计卡 + 热门标签 + 我的待办 +
 * 热门方案 + 公告 + 讨论热帖，全部从既有表只读聚合，不建新表。
 */
public interface HomeService {

    HomeDashboardVO dashboard();
}
