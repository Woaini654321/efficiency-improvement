package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnouncementPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OperationAnnouncePageDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnounceStatsVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnounceDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnouncePageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/**
 * 公告业务。前台（登录即可、只出已发布）与运营（admin 全量）两套读，运营独占写。
 */
public interface AnnouncementService {

    // ---------- 前台（announcement/*）----------

    /** 前台分页：只出 published，置顶排前再按 published_at 倒序。登录即可，不校业务角色。 */
    PageVO<AnnouncementPageVO> frontPage(AnnouncementPageDTO dto);

    /** 前台详情：仅 published 可见，读取时原子自增 view_count；未发布或不存在抛 NOT_FOUND。 */
    AnnouncementDetailVO frontDetail(Long id);

    // ---------- 运营（operation/announce/*，全部 requireAnyRole(ADMIN)）----------

    PageVO<OperationAnnouncePageVO> operationPage(OperationAnnouncePageDTO dto);

    OperationAnnounceDetailVO operationDetail(Long id);

    Long create(AnnounceCreateDTO dto);

    void update(AnnounceUpdateDTO dto);

    /** 状态机：draft→published(补 published_at)/published→archived/archived→published(不改 published_at)。 */
    void changeStatus(Long id, String status);

    /** 物理删除（announcement 无逻辑删除列 deleted）。 */
    void delete(Long id);

    /** 各状态聚合计数 + 累计浏览数。 */
    AnnounceStatsVO stats();
}
