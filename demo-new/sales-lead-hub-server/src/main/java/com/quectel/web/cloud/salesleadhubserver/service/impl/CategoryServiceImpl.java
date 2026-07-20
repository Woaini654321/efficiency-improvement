package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.annotation.AuditAction;
import com.quectel.web.cloud.salesleadhubserver.convert.CategoryConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.OpportunityCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestCategoryDao;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryListDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.CategoryErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.CategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestCategoryDO;
import com.quectel.web.cloud.salesleadhubserver.service.CategoryService;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.vo.CategoryVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_PAGE_SIZE = 500;

    private final CategoryDao dao;
    private final RequestCategoryDao requestCategoryDao;
    private final OpportunityCategoryDao opportunityCategoryDao;
    private final CurrentUserResolver currentUser;
    private final CategoryConvert convert;

    public CategoryServiceImpl(CategoryDao dao,
                               RequestCategoryDao requestCategoryDao,
                               OpportunityCategoryDao opportunityCategoryDao,
                               CurrentUserResolver currentUser,
                               CategoryConvert convert) {
        this.dao = dao;
        this.requestCategoryDao = requestCategoryDao;
        this.opportunityCategoryDao = opportunityCategoryDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<CategoryVO> list(CategoryListDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 500 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);

        Page<CategoryDO> p = new Page<>(pageNumber, pageSize);
        IPage<CategoryDO> r = dao.lambdaQuery()
                .like(dto.getKeyword() != null && !dto.getKeyword().isEmpty(),
                        CategoryDO::getName, dto.getKeyword())
                .orderByAsc(CategoryDO::getParentId)
                .orderByAsc(CategoryDO::getSortOrder)
                .page(p);

        Map<Long, Integer> counts = contentCounts();
        List<CategoryVO> records = r.getRecords().stream()
                .map(d -> convert.toVO(d, counts.getOrDefault(d.getId(), 0)))
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    @AuditAction(actionType = "category_change", targetType = "Category")
    public Long create(CategoryCreateDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        CategoryDO d = convert.toCreateDO(dto);
        requireParentExists(d.getParentId());
        dao.save(d);
        return d.getId();
    }

    @Override
    @Transactional
    @AuditAction(actionType = "category_change", targetType = "Category")
    public void update(CategoryUpdateDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        CategoryDO existing = dao.getById(dto.getId());
        if (existing == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        convert.applyUpdate(dto, existing);
        if (dto.getId().equals(existing.getParentId())) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "父分类不能是自己");
        }
        requireParentExists(existing.getParentId());
        dao.updateById(existing);
    }

    @Override
    @Transactional
    @AuditAction(actionType = "category_change", targetType = "Category")
    public void delete(Long id) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        if (dao.getById(id) == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        // 防呆①：有子分类不能删（先删/移走子级），否则树断链
        long children = dao.count(new LambdaQueryWrapper<CategoryDO>()
                .eq(CategoryDO::getParentId, id));
        if (children > 0) {
            throw new BaseException(CategoryErrorCode.HAS_CHILDREN, "尚有 " + children + " 个子分类，不能删除");
        }
        // 防呆②：被内容引用不能删（否则留下悬空外键），建议改为停用
        long inUse = requestCategoryDao.count(new LambdaQueryWrapper<RequestCategoryDO>()
                        .eq(RequestCategoryDO::getCategoryId, id))
                + opportunityCategoryDao.count(new LambdaQueryWrapper<OpportunityCategoryDO>()
                        .eq(OpportunityCategoryDO::getCategoryId, id));
        if (inUse > 0) {
            throw new BaseException(CategoryErrorCode.IN_USE, "已被 " + inUse + " 条内容引用，请改为停用");
        }
        dao.removeById(id);
    }

    @Override
    @Transactional
    @AuditAction(actionType = "category_change", targetType = "Category")
    public void changeActive(Long id, boolean active) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        CategoryDO d = dao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        d.setIsActive(active ? 1 : 0);
        dao.updateById(d);
    }

    // ---------- private ----------

    private void requireParentExists(Long parentId) {
        if (parentId != null && dao.getById(parentId) == null) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "父分类不存在：" + parentId);
        }
    }

    /**
     * 各分类的内容引用计数（商机+需求关联表合并）。
     *
     * <p>拉全关联行在内存聚合——分类是运营维护页，关联行量级 = 内容数×分类数(≤5)，
     * 万级以内没有优化必要；若未来量级上来，改 groupBy SQL。</p>
     */
    private Map<Long, Integer> contentCounts() {
        Map<Long, Integer> m = new HashMap<>();
        requestCategoryDao.list().forEach(r ->
                m.merge(r.getCategoryId(), 1, Integer::sum));
        opportunityCategoryDao.list().forEach(r ->
                m.merge(r.getCategoryId(), 1, Integer::sum));
        return m;
    }
}
