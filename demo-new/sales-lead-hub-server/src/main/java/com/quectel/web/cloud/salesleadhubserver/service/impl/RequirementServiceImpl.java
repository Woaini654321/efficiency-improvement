package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.security.utils.SecurityUtils;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.RequirementConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.RequirementUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.RequirementErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.RequirementService;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.RequirementPageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequirementServiceImpl implements RequirementService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private final RequirementDao dao;
    private final CategoryDao categoryDao;
    private final RequestCategoryDao requestCategoryDao;
    private final CurrentUserResolver currentUser;
    private final RequirementConvert convert;

    public RequirementServiceImpl(RequirementDao dao,
                                  CategoryDao categoryDao,
                                  RequestCategoryDao requestCategoryDao,
                                  CurrentUserResolver currentUser,
                                  RequirementConvert convert) {
        this.dao = dao;
        this.categoryDao = categoryDao;
        this.requestCategoryDao = requestCategoryDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional
    public Long create(RequirementCreateDTO dto) {
        // 鉴权在最前：无权发布的人不该看到任何后续校验的细节
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        validateVisibility(dto);
        List<Long> catIds = parseIds(dto.getCategoryIds());

        OpportunityRequestDO d = convert.toCreateDO(dto);
        fillPublisher(d, me);
        d.setStatus("Pending");
        d.setSlaStatus("normal");
        d.setEscalationLevel("L0");
        d.setViewCount(0);
        d.setResponseCount(0);
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
    public void update(RequirementUpdateDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        validateVisibility(dto);
        List<Long> catIds = parseIds(dto.getCategoryIds());

        OpportunityRequestDO existing = dao.getById(dto.getId());
        if (existing == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        // 管理员可改任何人的需求；其余角色只能改自己发布的
        boolean isOwner = existing.getPublisherId() != null
                && existing.getPublisherId().equals(me.getId());
        if (!isOwner && !CurrentUserResolver.ROLE_ADMIN.equals(me.getRole())) {
            throw new BaseException(RequirementErrorCode.NOT_OWNER, "只能修改自己发布的需求");
        }

        convert.applyUpdate(dto, existing);             // 含客户端带上来的 version
        existing.setCategoryNames(resolveCategoryNames(catIds));
        boolean ok = dao.updateById(existing);          // 乐观锁：WHERE id=? AND version=?
        if (!ok) {
            throw new BaseException(RequirementErrorCode.VERSION_CONFLICT, "数据已被他人修改，请刷新后重试");
        }
        // 同一 @Transactional 内重写关联表：主表更新失败已抛异常，走不到这里
        writeJoinRows(dto.getId(), catIds);
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<RequirementPageVO> page(RequirementPageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        Long uid = SecurityUtils.getCurrentUserId();

        Page<OpportunityRequestDO> p = new Page<>(pageNumber, pageSize);
        IPage<OpportunityRequestDO> r = dao.lambdaQuery()
                .like(dto.getKeyword() != null, OpportunityRequestDO::getTitle, dto.getKeyword())
                .eq(dto.getStatus() != null, OpportunityRequestDO::getStatus, dto.getStatus())
                .eq(dto.getUrgency() != null, OpportunityRequestDO::getUrgency, dto.getUrgency())
                // 最小可见性收敛：公开的、或自己发布的。
                // dept/personnel 的精确成员解析属已知缺口，见计划 Task 10 Step 7。
                .and(w -> w.eq(OpportunityRequestDO::getVisibilityScope, "all")
                        .or().eq(OpportunityRequestDO::getPublisherId, uid))
                .orderByDesc(OpportunityRequestDO::getCreateTime)
                .page(p);

        List<RequirementPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public RequirementDetailVO detail(Long id) {
        OpportunityRequestDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        Long uid = SecurityUtils.getCurrentUserId();
        boolean visible = "all".equals(d.getVisibilityScope())
                || (d.getPublisherId() != null && d.getPublisherId().equals(uid));
        if (!visible) {
            throw new BaseException(ErrorCode.FORBIDDEN, "无权查看该需求");
        }
        return convert.toDetailVO(d);
    }

    /**
     * 回填发布人快照，防客户端伪造。
     *
     * <p>全部取自本地 {@code sys_user}（它已由 {@link CurrentUserResolver} 解析出来，
     * 不再重复查库）。UAA 只提供 7 个字段且不含部门，本地档案才是组织信息的 SSOT；
     * 且 {@code publisher_id} 是指向 {@code sys_user.id} 的 FK，用同一行的 id 才引用一致。</p>
     *
     * <p>这里不再有"本地查不到就降级"的分支——鉴权阶段已经 fail-closed 拦掉了
     * 未开通的账号，能走到这里的一定有本地档案。</p>
     */
    private void fillPublisher(OpportunityRequestDO d, SysUserDO me) {
        d.setPublisherId(me.getId());
        d.setPublisherName(me.getName());
        d.setDepartmentId(me.getDepartmentId());
        d.setPublisherDeptName(me.getDepartmentName());
    }

    /**
     * 前端 ID 一律是 string（Long→String 约定），此处转 Long。
     *
     * <p>非数字直接判参数非法而非跳过——静默丢分类会让用户以为选上了、
     * 实际落库为空，是最难被发现的一类缺陷。</p>
     */
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

    /**
     * 重写需求与分类的关联行。
     *
     * <p>关联表是复合主键，<b>禁用 removeById/getById</b>（只按单列匹配，语义是错的），
     * 故按 requestId 全删重写。</p>
     */
    private void writeJoinRows(Long requestId, List<Long> catIds) {
        requestCategoryDao.remove(new LambdaQueryWrapper<RequestCategoryDO>()
                .eq(RequestCategoryDO::getRequestId, requestId));
        if (catIds.isEmpty()) {
            return;
        }
        List<RequestCategoryDO> rows = catIds.stream().map(cid -> {
            RequestCategoryDO r = new RequestCategoryDO();
            r.setRequestId(requestId);
            r.setCategoryId(cid);
            return r;
        }).collect(Collectors.toList());
        requestCategoryDao.saveBatch(rows);
    }

    private void validateVisibility(RequirementCreateDTO dto) {
        if (!"all".equals(dto.getVisibilityType())
                && (dto.getVisibilityValues() == null || dto.getVisibilityValues().isEmpty())) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "选择部门/人员可见时必须指定可见范围");
        }
    }
}
