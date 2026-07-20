package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.annotation.AuditAction;
import com.quectel.web.cloud.salesleadhubserver.convert.OpportunityConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.OpportunityUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.OpportunityErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.OpportunityService;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.OpportunityPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OpportunityServiceImpl implements OpportunityService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final String STATUS_ARCHIVED = "archived";

    private final OpportunityDao dao;
    private final CategoryDao categoryDao;
    private final OpportunityCategoryDao opportunityCategoryDao;
    private final SysUserDao sysUserDao;
    private final CurrentUserResolver currentUser;
    private final OpportunityConvert convert;

    public OpportunityServiceImpl(OpportunityDao dao,
                                  CategoryDao categoryDao,
                                  OpportunityCategoryDao opportunityCategoryDao,
                                  SysUserDao sysUserDao,
                                  CurrentUserResolver currentUser,
                                  OpportunityConvert convert) {
        this.dao = dao;
        this.categoryDao = categoryDao;
        this.opportunityCategoryDao = opportunityCategoryDao;
        this.sysUserDao = sysUserDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional
    public Long create(OpportunityCreateDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        validateStatusForWrite(dto);
        List<Long> catIds = parseIds(dto.getCategoryIds());
        SysUserDO publisher = resolvePublisher(dto.getPublisherId(), me);

        OpportunityDO d = convert.toCreateDO(dto);
        fillPublisher(d, publisher);
        d.setViewCount(0);
        d.setLikeCount(0);
        d.setCollectCount(0);
        d.setCommentCount(0);
        d.setCategoryNames(resolveCategoryNames(catIds));

        dao.save(d);
        writeJoinRows(d.getId(), catIds);
        return d.getId();
    }

    @Override
    @Transactional
    public void update(OpportunityUpdateDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        validateStatusForWrite(dto);
        List<Long> catIds = parseIds(dto.getCategoryIds());

        OpportunityDO existing = dao.getById(dto.getId());
        if (existing == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "商机不存在");
        }
        requireOwnerOrAdmin(existing, me, "只能修改自己发布的商机");

        convert.applyUpdate(dto, existing);             // 含客户端带上来的 version
        existing.setCategoryNames(resolveCategoryNames(catIds));
        boolean ok = dao.updateById(existing);          // 乐观锁：WHERE id=? AND version=?
        if (!ok) {
            throw new BaseException(OpportunityErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
        writeJoinRows(dto.getId(), catIds);
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<OpportunityPageVO> page(OpportunityPageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        Long uid = SecurityUtils.getCurrentUserId();

        Page<OpportunityDO> p = new Page<>(pageNumber, pageSize);
        IPage<OpportunityDO> r = dao.lambdaQuery()
                .like(dto.getKeyword() != null, OpportunityDO::getTitle, dto.getKeyword())
                .eq(dto.getType() != null, OpportunityDO::getType, dto.getType())
                .eq(dto.getStatus() != null, OpportunityDO::getStatus, dto.getStatus())
                // 草稿只有本人可见；published/archived 全员可见（恢复入口的展示由前端按钮控制，
                // 真正的恢复权限在 changeStatus 里校验）
                .and(w -> w.ne(OpportunityDO::getStatus, STATUS_DRAFT)
                        .or().eq(OpportunityDO::getPublisherId, uid))
                .orderByDesc(OpportunityDO::getCreateTime)
                .page(p);

        List<OpportunityPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public OpportunityDetailVO detail(Long id) {
        OpportunityDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "商机不存在");
        }
        if (STATUS_DRAFT.equals(d.getStatus())) {
            Long uid = SecurityUtils.getCurrentUserId();
            boolean owner = d.getPublisherId() != null && d.getPublisherId().equals(uid);
            if (!owner) {
                throw new BaseException(ErrorCode.FORBIDDEN, "草稿仅发布人本人可见");
            }
        }
        return convert.toDetailVO(d);
    }

    @Override
    @Transactional
    @AuditAction(actionType = AuditAction.AUTO_STATUS, targetType = "Opportunity")
    public void changeStatus(Long id, String status) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        OpportunityDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "商机不存在");
        }

        if (STATUS_ARCHIVED.equals(status)) {
            // 下架：仅 published 可下架；发布人本人或管理员
            if (!STATUS_PUBLISHED.equals(d.getStatus())) {
                throw new BaseException(OpportunityErrorCode.ILLEGAL_TRANSITION, "仅已发布的商机可下架");
            }
            requireOwnerOrAdmin(d, me, "只能下架自己发布的商机");
            d.setStatus(STATUS_ARCHIVED);
            d.setArchivedBy(me.getId());
        } else if (STATUS_PUBLISHED.equals(status)) {
            // 恢复：仅 archived 可恢复；谁下架谁恢复（或管理员）
            if (!STATUS_ARCHIVED.equals(d.getStatus())) {
                throw new BaseException(OpportunityErrorCode.ILLEGAL_TRANSITION, "仅已下架的商机可恢复");
            }
            boolean archiver = d.getArchivedBy() != null && d.getArchivedBy().equals(me.getId());
            if (!archiver && !CurrentUserResolver.ROLE_ADMIN.equals(me.getRole())) {
                throw new BaseException(OpportunityErrorCode.NOT_ARCHIVER, "只有下架人本人或管理员可恢复上架");
            }
            d.setStatus(STATUS_PUBLISHED);
            d.setArchivedBy(null);
        } else {
            throw new BaseException(ErrorCode.PARAM_INVALID, "非法目标状态：" + status);
        }

        boolean ok = dao.updateById(d);
        if (!ok) {
            throw new BaseException(OpportunityErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
    }

    // ---------- private ----------

    /**
     * 状态与按状态的条件必填校验。
     *
     * <p>与前端行为逐字对齐：存草稿只要求标题（DTO @NotBlank 已保证）；
     * 发布要求分类 1~5 个且正文非空。archived 不许从 create/update 进入，
     * 只能走 changeStatus——否则「下架人」语义会被绕过。</p>
     */
    private void validateStatusForWrite(OpportunityCreateDTO dto) {
        String s = dto.getStatus();
        if (!STATUS_DRAFT.equals(s) && !STATUS_PUBLISHED.equals(s)) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "status 只能是 draft/published：" + s);
        }
        if (STATUS_PUBLISHED.equals(s)) {
            int catCount = dto.getCategoryIds() == null ? 0 : dto.getCategoryIds().size();
            if (catCount < 1 || catCount > 5) {
                throw new BaseException(ErrorCode.PARAM_INVALID, "发布须选择 1~5 个分类");
            }
            if (dto.getContent() == null || dto.getContent().replaceAll("<[^>]*>", "").trim().isEmpty()) {
                throw new BaseException(ErrorCode.PARAM_INVALID, "发布须填写正文");
            }
        }
    }

    /**
     * 解析发布主体：默认本人；代发布（publisherId ≠ 本人）要求 product_manager/admin，
     * 且目标账号必须已开通且在职——快照字段全部取自目标本地档案。
     */
    private SysUserDO resolvePublisher(String publisherId, SysUserDO me) {
        if (publisherId == null || publisherId.isEmpty()
                || publisherId.equals(String.valueOf(me.getId()))) {
            return me;
        }
        if (!CurrentUserResolver.ROLE_PRODUCT_MANAGER.equals(me.getRole())
                && !CurrentUserResolver.ROLE_ADMIN.equals(me.getRole())) {
            throw new BaseException(ErrorCode.FORBIDDEN, "仅产品经理或管理员可代发布");
        }
        Long targetId;
        try {
            targetId = Long.valueOf(publisherId);
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "代发布目标用户 id 非法：" + publisherId);
        }
        SysUserDO target = sysUserDao.getById(targetId);
        if (target == null || !"active".equals(target.getStatus())) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "代发布目标用户不存在或已停用");
        }
        return target;
    }

    /** 回填发布人快照（防客户端伪造），全部取自本地 sys_user 同一行，理由见 RequirementServiceImpl。 */
    private void fillPublisher(OpportunityDO d, SysUserDO publisher) {
        d.setPublisherId(publisher.getId());
        d.setPublisherName(publisher.getName());
        d.setDepartmentId(publisher.getDepartmentId());
        d.setPublisherDeptName(publisher.getDepartmentName());
    }

    /** 前端 ID 一律 string，非数字直接判非法而非静默丢弃（理由见 RequirementServiceImpl）。 */
    private List<Long> parseIds(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return raw.stream().map(Long::valueOf).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "分类 id 非法：" + raw);
        }
    }

    /** 分类名快照：列表页直接读主表这一列，避免每行联查。 */
    private List<String> resolveCategoryNames(List<Long> catIds) {
        if (catIds.isEmpty()) {
            return Collections.emptyList();
        }
        return categoryDao.listByIds(catIds).stream()
                .map(CategoryDO::getName)
                .collect(Collectors.toList());
    }

    /** 复合主键关联表：按 opportunityId 全删重写，禁 removeById（见 Dao javadoc）。 */
    private void writeJoinRows(Long opportunityId, List<Long> catIds) {
        opportunityCategoryDao.remove(new LambdaQueryWrapper<OpportunityCategoryDO>()
                .eq(OpportunityCategoryDO::getOpportunityId, opportunityId));
        if (catIds.isEmpty()) {
            return;
        }
        List<OpportunityCategoryDO> rows = catIds.stream().map(cid -> {
            OpportunityCategoryDO r = new OpportunityCategoryDO();
            r.setOpportunityId(opportunityId);
            r.setCategoryId(cid);
            return r;
        }).collect(Collectors.toList());
        opportunityCategoryDao.saveBatch(rows);
    }

    private void requireOwnerOrAdmin(OpportunityDO d, SysUserDO me, String message) {
        boolean isOwner = d.getPublisherId() != null && d.getPublisherId().equals(me.getId());
        if (!isOwner && !CurrentUserResolver.ROLE_ADMIN.equals(me.getRole())) {
            throw new BaseException(OpportunityErrorCode.NOT_OWNER, message);
        }
    }
}
