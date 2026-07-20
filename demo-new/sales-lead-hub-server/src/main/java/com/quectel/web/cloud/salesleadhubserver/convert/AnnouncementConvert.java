package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnounceDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnouncePageVO;
import org.springframework.stereotype.Component;

/**
 * 手写 公告 DTO/VO ⇄ DO 映射（不引 MapStruct）。
 *
 * <p>审计字段（createBy/updateBy/createTime/updateTime）与 publisher* 一律不在此处赋值：
 * 前者由框架 {@code SecurityMetaObjectHandler} 填充，后者由 service 回填（防客户端伪造）。
 * status/viewCount/publishedAt 等业务字段由 service 按状态机控制，convert 不碰。</p>
 *
 * <p>前台 VO 与运营 VO 是两套扁平契约（不互相继承）：前台窄（无 content 列表/无 version/无 banner），
 * 运营宽（全字段）；四个 toXxxVO 逐字段显式赋值，任一契约漂移编译器能直接指到。</p>
 */
@Component
public class AnnouncementConvert {

    /** is_pinned/banner_enabled 存储为 TINYINT(0/1)，前端读写为布尔。 */
    private int boolToInt(Boolean b) {
        return Boolean.TRUE.equals(b) ? 1 : 0;
    }

    private boolean intToBool(Integer i) {
        return i != null && i == 1;
    }

    // ---------- DTO → DO ----------

    public AnnouncementDO toCreateDO(AnnounceCreateDTO dto) {
        AnnouncementDO d = new AnnouncementDO();
        d.setTitle(dto.getTitle());
        d.setType(dto.getType());
        d.setPriority(dto.getPriority());
        d.setContent(dto.getContent());
        d.setIsPinned(boolToInt(dto.getIsPinned()));
        d.setBannerEnabled(boolToInt(dto.getBannerEnabled()));
        return d;
    }

    /**
     * 把可改字段与 version 灌入<b>已从库中加载</b>的 DO。
     *
     * <p>必须回填客户端带上来的 version 参与 {@code WHERE version=?}，沿用库里的会让任何
     * 陈旧提交都被当成最新提交放行。status/publishedAt/viewCount 不在此更新（走 changeStatus 与自增）。</p>
     */
    public void applyUpdate(AnnounceUpdateDTO dto, AnnouncementDO d) {
        d.setTitle(dto.getTitle());
        d.setType(dto.getType());
        d.setPriority(dto.getPriority());
        d.setContent(dto.getContent());
        d.setIsPinned(boolToInt(dto.getIsPinned()));
        d.setBannerEnabled(boolToInt(dto.getBannerEnabled()));
        d.setVersion(dto.getVersion());
    }

    // ---------- DO → 前台 VO ----------

    public AnnouncementPageVO toFrontPageVO(AnnouncementDO d) {
        AnnouncementPageVO v = new AnnouncementPageVO();
        v.setAnnouncementId(d.getId());
        v.setTitle(d.getTitle());
        v.setType(d.getType());
        v.setStatus(d.getStatus());
        v.setPriority(d.getPriority());
        v.setIsPinned(intToBool(d.getIsPinned()));
        v.setPublisherName(d.getPublisherName());
        v.setViewCount(d.getViewCount());
        v.setPublishedAt(d.getPublishedAt());
        return v;
    }

    public AnnouncementDetailVO toFrontDetailVO(AnnouncementDO d) {
        AnnouncementDetailVO v = new AnnouncementDetailVO();
        v.setAnnouncementId(d.getId());
        v.setTitle(d.getTitle());
        v.setContent(d.getContent());
        v.setType(d.getType());
        v.setStatus(d.getStatus());
        v.setPriority(d.getPriority());
        v.setIsPinned(intToBool(d.getIsPinned()));
        v.setPublisherName(d.getPublisherName());
        v.setViewCount(d.getViewCount());
        v.setPublishedAt(d.getPublishedAt());
        return v;
    }

    // ---------- DO → 运营 VO ----------

    public OperationAnnouncePageVO toOpPageVO(AnnouncementDO d) {
        OperationAnnouncePageVO v = new OperationAnnouncePageVO();
        v.setAnnouncementId(d.getId());
        v.setTitle(d.getTitle());
        v.setType(d.getType());
        v.setStatus(d.getStatus());
        v.setPriority(d.getPriority());
        v.setIsPinned(intToBool(d.getIsPinned()));
        v.setPublisherName(d.getPublisherName());
        v.setViewCount(d.getViewCount());
        v.setContent(d.getContent());
        v.setBannerEnabled(intToBool(d.getBannerEnabled()));
        v.setVersion(d.getVersion());
        v.setCreatedAt(d.getCreateTime());
        v.setPublishedAt(d.getPublishedAt());
        return v;
    }

    public OperationAnnounceDetailVO toOpDetailVO(AnnouncementDO d) {
        OperationAnnounceDetailVO v = new OperationAnnounceDetailVO();
        v.setAnnouncementId(d.getId());
        v.setTitle(d.getTitle());
        v.setType(d.getType());
        v.setStatus(d.getStatus());
        v.setPriority(d.getPriority());
        v.setIsPinned(intToBool(d.getIsPinned()));
        v.setPublisherName(d.getPublisherName());
        v.setViewCount(d.getViewCount());
        v.setContent(d.getContent());
        v.setBannerEnabled(intToBool(d.getBannerEnabled()));
        v.setVersion(d.getVersion());
        v.setCreatedAt(d.getCreateTime());
        v.setPublishedAt(d.getPublishedAt());
        return v;
    }
}
