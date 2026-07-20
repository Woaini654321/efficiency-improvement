package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.annotation.AuditAction;
import com.quectel.web.cloud.salesleadhubserver.convert.AnnouncementConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.AnnouncementDao;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnounceUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnouncementPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OperationAnnouncePageDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.AnnouncementErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.AnnouncementDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.AnnouncementService;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnounceStatsVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnounceDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OperationAnnouncePageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_ARCHIVED = "archived";

    private final AnnouncementDao dao;
    private final CurrentUserResolver currentUser;
    private final AnnouncementConvert convert;

    public AnnouncementServiceImpl(AnnouncementDao dao,
                                   CurrentUserResolver currentUser,
                                   AnnouncementConvert convert) {
        this.dao = dao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    // ==================== 前台 ====================

    @Override
    @Transactional(readOnly = true)
    public PageVO<AnnouncementPageVO> frontPage(AnnouncementPageDTO dto) {
        Page<AnnouncementDO> p = buildPage(dto.getPageNumber(), dto.getPageSize());
        IPage<AnnouncementDO> r = dao.lambdaQuery()
                // 前台恒定只出已发布，不接受客户端 status 参数（避免越权拉草稿/归档）
                .eq(AnnouncementDO::getStatus, STATUS_PUBLISHED)
                .like(dto.getKeyword() != null, AnnouncementDO::getTitle, dto.getKeyword())
                .eq(dto.getType() != null, AnnouncementDO::getType, dto.getType())
                // 置顶排前，再按发布时间倒序
                .orderByDesc(AnnouncementDO::getIsPinned)
                .orderByDesc(AnnouncementDO::getPublishedAt)
                .page(p);

        List<AnnouncementPageVO> records = r.getRecords().stream()
                .map(convert::toFrontPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public AnnouncementDetailVO frontDetail(Long id) {
        AnnouncementDO d = dao.getById(id);
        // 未发布或不存在对前台一律 NOT_FOUND（不泄露草稿/归档的存在）
        if (d == null || !STATUS_PUBLISHED.equals(d.getStatus())) {
            throw new BaseException(ErrorCode.NOT_FOUND, "公告不存在或未发布");
        }
        // 原子自增浏览数：UpdateWrapper setSql 不带实体，不触发乐观锁 version 校验，
        // 也不会因并发读多写而丢计数（先查判可见，再无条件自增）
        dao.lambdaUpdate()
                .setSql("view_count = view_count + 1")
                .eq(AnnouncementDO::getId, id)
                .update();
        // 回显 +1 后的值（避免再查一次库）
        d.setViewCount((d.getViewCount() == null ? 0 : d.getViewCount()) + 1);
        return convert.toFrontDetailVO(d);
    }

    // ==================== 运营 ====================

    @Override
    @Transactional(readOnly = true)
    public PageVO<OperationAnnouncePageVO> operationPage(OperationAnnouncePageDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        Page<AnnouncementDO> p = buildPage(dto.getPageNumber(), dto.getPageSize());
        IPage<AnnouncementDO> r = dao.lambdaQuery()
                .like(dto.getKeyword() != null, AnnouncementDO::getTitle, dto.getKeyword())
                .eq(dto.getType() != null, AnnouncementDO::getType, dto.getType())
                // 运营端返回全部状态（含草稿）；有 status 参数才过滤
                .eq(dto.getStatus() != null, AnnouncementDO::getStatus, dto.getStatus())
                .orderByDesc(AnnouncementDO::getIsPinned)
                .orderByDesc(AnnouncementDO::getCreateTime)
                .page(p);

        List<OperationAnnouncePageVO> records = r.getRecords().stream()
                .map(convert::toOpPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public OperationAnnounceDetailVO operationDetail(Long id) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        AnnouncementDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return convert.toOpDetailVO(d);
    }

    @Override
    @Transactional
    public Long create(AnnounceCreateDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        AnnouncementDO d = convert.toCreateDO(dto);
        // 前端 create payload 不含 status（存草稿/发布共用同一调用），一律落草稿；
        // 发布走独立的 changeStatus 端点（见 AnnounceCreateDTO javadoc）
        d.setStatus(STATUS_DRAFT);
        d.setViewCount(0);
        d.setPublishedAt(null);
        // 发布人快照取自本地 sys_user 同一行（防客户端伪造）
        d.setPublisherId(me.getId());
        d.setPublisherName(me.getName());
        dao.save(d);
        return d.getId();
    }

    @Override
    @Transactional
    public void update(AnnounceUpdateDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        AnnouncementDO existing = dao.getById(dto.getId());
        if (existing == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        // 只改内容字段，不动 status/publishedAt/viewCount/publisher*
        convert.applyUpdate(dto, existing);        // 含客户端带上来的 version
        boolean ok = dao.updateById(existing);     // 乐观锁：WHERE id=? AND version=?
        if (!ok) {
            throw new BaseException(AnnouncementErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
    }

    @Override
    @Transactional
    @AuditAction(actionType = AuditAction.AUTO_STATUS, targetType = "Announcement")
    public void changeStatus(Long id, String status) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        AnnouncementDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        String from = d.getStatus();

        if (STATUS_PUBLISHED.equals(status)) {
            if (STATUS_DRAFT.equals(from)) {
                // 首次发布：补发布时间
                d.setStatus(STATUS_PUBLISHED);
                d.setPublishedAt(LocalDateTime.now());
            } else if (STATUS_ARCHIVED.equals(from)) {
                // 恢复上架：不改 published_at（保留初次发布时间）
                d.setStatus(STATUS_PUBLISHED);
            } else {
                throw new BaseException(AnnouncementErrorCode.ILLEGAL_TRANSITION,
                        "仅草稿或已归档的公告可发布/恢复：当前 " + from);
            }
        } else if (STATUS_ARCHIVED.equals(status)) {
            // 下架：仅已发布可下架
            if (!STATUS_PUBLISHED.equals(from)) {
                throw new BaseException(AnnouncementErrorCode.ILLEGAL_TRANSITION,
                        "仅已发布的公告可下架：当前 " + from);
            }
            d.setStatus(STATUS_ARCHIVED);
        } else {
            throw new BaseException(ErrorCode.PARAM_INVALID, "非法目标状态：" + status);
        }

        boolean ok = dao.updateById(d);
        if (!ok) {
            throw new BaseException(AnnouncementErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
    }

    @Override
    @Transactional
    @AuditAction(actionType = "delete", targetType = "Announcement")
    public void delete(Long id) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        // announcement 无逻辑删除列 deleted → removeById 即物理删；
        // 审计（谁删了什么）由 audit_log 后续切面统一补，此处不额外落痕
        dao.removeById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AnnounceStatsVO stats() {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        // 公告量级小，单次全量扫描聚合即可（避免 4 条 COUNT/SUM 往返）；量级上来后可改 SQL 聚合
        List<AnnouncementDO> all = dao.list();
        int total = all.size();
        int published = 0;
        int draft = 0;
        long totalViews = 0;
        for (AnnouncementDO d : all) {
            if (STATUS_PUBLISHED.equals(d.getStatus())) {
                published++;
            } else if (STATUS_DRAFT.equals(d.getStatus())) {
                draft++;
            }
            totalViews += d.getViewCount() == null ? 0 : d.getViewCount();
        }
        AnnounceStatsVO vo = new AnnounceStatsVO();
        vo.setTotal(total);
        vo.setPublished(published);
        vo.setDraft(draft);
        vo.setTotalViews((int) totalViews);
        return vo;
    }

    // ---------- private ----------

    private Page<AnnouncementDO> buildPage(Integer pageNumber, Integer pageSize) {
        int pn = pageNumber == null ? 1 : pageNumber;
        int ps = pageSize == null ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        return new Page<>(pn, ps);
    }
}
