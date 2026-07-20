package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.SlaPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.SlaUrgeDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaMetaVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaRequestVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaStatsVO;

/**
 * 需求时效（SLA）监控。全部端点仅运营（admin）可用；均为对 opportunity_request 的只读聚合，
 * SLA 状态/升级级数/剩余文案一律按服务器当前时间实时派生，不建新表、不依赖定时任务。
 */
public interface SlaService {

    /** 分页查询需求时效列表（slaStatus 按实时派生结果过滤）。 */
    PageVO<SlaRequestVO> page(SlaPageDTO dto);

    /** 时效统计卡（总数/及时率/已响应/最长超时）。 */
    SlaStatsVO stats();

    /** 催办元数据（产品线负责人候选 + 邮件通知人候选）。 */
    SlaMetaVO meta();

    /** 催办：向解析出的本地接收人写 in_app 通知（feishu/email 待接消息中台）。 */
    void urge(SlaUrgeDTO dto);
}
