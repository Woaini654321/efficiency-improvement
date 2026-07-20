package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.ViewLogDO;

/**
 * 浏览记录（view_log）数据访问。
 *
 * <p>home「近7天活跃用户」与 dashboard 的 UV/PV/页面热力/时段活跃全部由本表聚合。
 * 数据量小（单条浏览 24h 去重），聚合统一走 {@code list(wrapper)} 后在 service 层
 * 用 Stream 现算，不写复杂原生 SQL（与迁移方案 Phase 5「count/selectList 直查」一致）。</p>
 */
public interface ViewLogDao extends IService<ViewLogDO> {
}
