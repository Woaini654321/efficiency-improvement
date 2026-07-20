package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.annotation.AuditAction;
import com.quectel.web.cloud.salesleadhubserver.convert.AuditConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditChangePinDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditPageDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.AuditErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.AuditService;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营内容审核服务实现。跨 opportunity 与 opportunity_request 两张表。
 *
 * <p>全部端点仅 admin 可调：每个方法开头 {@code currentUser.requireAnyRole(ROLE_ADMIN)}
 * fail-closed。审计行（谁在何时下架/删除了哪条）由后续 AOP 切面统一补，本服务不写审计表。</p>
 *
 * <p><b>为什么 page 不写 UNION SQL：</b>两表结构差异大（商机无 urgency/industry，需求有
 * SLA 派生列），UNION 需硬凑列且难维护。改为按 contentType 分流：传单类型走对应 Dao 的
 * SQL 分页；不传（前端运营页实际行为）时两表各自 list 后在内存合并、排序、分页。
 * <b>权衡</b>：合并模式全量拉两表进内存，现阶段两表数据量小（各数十行）可接受；数据量上
 * 千后需改为 DB 层 UNION 视图或物化排序键，届时本方法是唯一改动点。</p>
 */
@Service
public class AuditServiceImpl implements AuditService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_ARCHIVED = "archived";
    /** 需求侧运营强制下架 = 关闭。需求表无 archived 态，Closed 是其"下架"等价。 */
    private static final String STATUS_CLOSED = "Closed";

    /**
     * 审核列表排序：置顶优先 → sort_no 升序 → 发布时间倒序。
     *
     * <p>对齐前端运营页展示：pinned 组在前，sort_no 小者在前（前端 {@code a.sortNo - b.sortNo}），
     * 同序号按时间新者在前。合并模式在内存用它，单表模式在 SQL orderBy 用等价三段。</p>
     */
    private static final Comparator<AuditPageVO> DISPLAY_ORDER =
            Comparator.comparing((AuditPageVO v) -> Boolean.TRUE.equals(v.getIsPinned()) ? 0 : 1)
                    .thenComparing(v -> v.getSortNo() == null ? 0 : v.getSortNo())
                    .thenComparing(AuditPageVO::getPublishedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()));

    private final OpportunityDao opportunityDao;
    private final RequirementDao requirementDao;
    private final CurrentUserResolver currentUser;
    private final AuditConvert convert;

    public AuditServiceImpl(OpportunityDao opportunityDao,
                            RequirementDao requirementDao,
                            CurrentUserResolver currentUser,
                            AuditConvert convert) {
        this.opportunityDao = opportunityDao;
        this.requirementDao = requirementDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<AuditPageVO> page(AuditPageDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        String keyword = dto.getKeyword();
        String status = dto.getStatus();
        String type = dto.getContentType();

        if (AuditConvert.TYPE_OPPORTUNITY.equals(type)) {
            return pageOpportunity(pageNumber, pageSize, keyword, status);
        }
        if (AuditConvert.TYPE_REQUEST.equals(type)) {
            return pageRequest(pageNumber, pageSize, keyword, status);
        }
        return pageMerged(pageNumber, pageSize, keyword, status);
    }

    /** 仅商机：SQL 分页。审核视图不含草稿（草稿未对外发布，无需审核）。 */
    private PageVO<AuditPageVO> pageOpportunity(int pageNumber, int pageSize, String keyword, String status) {
        Page<OpportunityDO> p = new Page<>(pageNumber, pageSize);
        IPage<OpportunityDO> r = opportunityDao.lambdaQuery()
                .like(keyword != null, OpportunityDO::getTitle, keyword)
                .eq(status != null, OpportunityDO::getStatus, status)
                .ne(OpportunityDO::getStatus, STATUS_DRAFT)
                .orderByDesc(OpportunityDO::getIsPinned)
                .orderByAsc(OpportunityDO::getSortNo)
                .orderByDesc(OpportunityDO::getCreateTime)
                .page(p);
        List<AuditPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    /** 仅需求：SQL 分页。 */
    private PageVO<AuditPageVO> pageRequest(int pageNumber, int pageSize, String keyword, String status) {
        Page<OpportunityRequestDO> p = new Page<>(pageNumber, pageSize);
        IPage<OpportunityRequestDO> r = requirementDao.lambdaQuery()
                .like(keyword != null, OpportunityRequestDO::getTitle, keyword)
                .eq(status != null, OpportunityRequestDO::getStatus, status)
                .orderByDesc(OpportunityRequestDO::getIsPinned)
                .orderByAsc(OpportunityRequestDO::getSortNo)
                .orderByDesc(OpportunityRequestDO::getCreateTime)
                .page(p);
        List<AuditPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    /** 两表合并：各自 list（商机去草稿）后在内存排序、分页。数据量小，见类 javadoc 权衡。 */
    private PageVO<AuditPageVO> pageMerged(int pageNumber, int pageSize, String keyword, String status) {
        List<AuditPageVO> all = new ArrayList<>();

        opportunityDao.lambdaQuery()
                .like(keyword != null, OpportunityDO::getTitle, keyword)
                .eq(status != null, OpportunityDO::getStatus, status)
                .ne(OpportunityDO::getStatus, STATUS_DRAFT)
                .list()
                .forEach(d -> all.add(convert.toPageVO(d)));

        requirementDao.lambdaQuery()
                .like(keyword != null, OpportunityRequestDO::getTitle, keyword)
                .eq(status != null, OpportunityRequestDO::getStatus, status)
                .list()
                .forEach(d -> all.add(convert.toPageVO(d)));

        all.sort(DISPLAY_ORDER);

        long total = all.size();
        int from = Math.max(0, (pageNumber - 1) * pageSize);
        if (from >= all.size()) {
            return new PageVO<>(new ArrayList<>(), total);
        }
        int to = Math.min(from + pageSize, all.size());
        return new PageVO<>(new ArrayList<>(all.subList(from, to)), total);
    }

    @Override
    @Transactional
    @AuditAction(actionType = AuditAction.AUTO_STATUS, targetType = "Content")
    public void changeStatus(Long id, String status) {
        SysUserDO me = currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        // 目标表由 status 取值判定：published/archived → 商机；Closed → 需求
        if (STATUS_ARCHIVED.equals(status) || STATUS_PUBLISHED.equals(status)) {
            changeOpportunityStatus(id, status, me);
        } else if (STATUS_CLOSED.equals(status)) {
            changeRequestStatus(id, status);
        } else {
            throw new BaseException(ErrorCode.PARAM_INVALID, "非法目标状态：" + status);
        }
    }

    /** 商机强制上/下架：复用 opportunity 模块语义（下架记 archived_by=当前运营，恢复清空）。 */
    private void changeOpportunityStatus(Long id, String status, SysUserDO me) {
        OpportunityDO d = opportunityDao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "商机不存在");
        }
        if (STATUS_ARCHIVED.equals(status)) {
            if (!STATUS_PUBLISHED.equals(d.getStatus())) {
                throw new BaseException(AuditErrorCode.ILLEGAL_TRANSITION, "仅已发布的商机可下架");
            }
            d.setStatus(STATUS_ARCHIVED);
            d.setArchivedBy(me.getId());
        } else {
            // 恢复：admin 无条件可恢复（archived_by 的 ALWAYS 策略保证清回 NULL）
            if (!STATUS_ARCHIVED.equals(d.getStatus())) {
                throw new BaseException(AuditErrorCode.ILLEGAL_TRANSITION, "仅已下架的商机可恢复");
            }
            d.setStatus(STATUS_PUBLISHED);
            d.setArchivedBy(null);
        }
        if (!opportunityDao.updateById(d)) {
            throw new BaseException(AuditErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
    }

    /** 需求强制关闭（= 下架）。需求表无 archived 态，Closed 是其唯一运营下架目标。 */
    private void changeRequestStatus(Long id, String status) {
        OpportunityRequestDO d = requirementDao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        d.setStatus(status);
        if (!requirementDao.updateById(d)) {
            throw new BaseException(AuditErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
    }

    @Override
    @Transactional
    public void changePin(AuditChangePinDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        // 无 contentType：按 id（雪花全局唯一）先探商机、再探需求
        OpportunityDO o = opportunityDao.getById(dto.getId());
        if (o != null) {
            o.setIsPinned(dto.getIsPinned());
            if (dto.getSortNo() != null) {
                o.setSortNo(dto.getSortNo());
            }
            if (!opportunityDao.updateById(o)) {
                throw new BaseException(AuditErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
            }
            return;
        }
        OpportunityRequestDO r = requirementDao.getById(dto.getId());
        if (r != null) {
            r.setIsPinned(dto.getIsPinned());
            if (dto.getSortNo() != null) {
                r.setSortNo(dto.getSortNo());
            }
            if (!requirementDao.updateById(r)) {
                throw new BaseException(AuditErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
            }
            return;
        }
        throw new BaseException(ErrorCode.NOT_FOUND, "内容不存在");
    }

    @Override
    @Transactional
    @AuditAction(actionType = "delete", targetType = "Content")
    public void delete(Long id) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        // 框架逻辑删（deleted='Y'）。按 id 探两表分流；审计行由 AOP 切面统一补。
        if (opportunityDao.getById(id) != null) {
            opportunityDao.removeById(id);
            return;
        }
        if (requirementDao.getById(id) != null) {
            requirementDao.removeById(id);
            return;
        }
        throw new BaseException(ErrorCode.NOT_FOUND, "内容不存在");
    }
}
