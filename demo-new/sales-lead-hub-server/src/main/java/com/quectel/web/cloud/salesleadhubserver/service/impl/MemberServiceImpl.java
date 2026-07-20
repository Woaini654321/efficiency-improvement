package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.MemberConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberAddDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.MemberErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.MemberService;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 产品线成员业务实现。
 *
 * <p>写操作（add/update）仅 admin：运营在后台维护产品线成员，业务角色查本地 sys_user.role；
 * 读操作（page/detail）放开给登录用户。核心规则：is_owner=1 即 SLA L1 升级人，一条产品线
 * 至多一个 owner，设 owner 前必须校验无其他 owner。user_name 是快照，从本地 sys_user 单行取
 * （禁 UAA），用户不存在或非在职拒绝。</p>
 */
@Service
public class MemberServiceImpl implements MemberService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final int OWNER = 1;

    private final ProductLineMemberDao memberDao;
    private final ProductLineDao productLineDao;
    private final SysUserDao sysUserDao;
    private final CurrentUserResolver currentUser;
    private final MemberConvert convert;

    public MemberServiceImpl(ProductLineMemberDao memberDao,
                             ProductLineDao productLineDao,
                             SysUserDao sysUserDao,
                             CurrentUserResolver currentUser,
                             MemberConvert convert) {
        this.memberDao = memberDao;
        this.productLineDao = productLineDao;
        this.sysUserDao = sysUserDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<MemberPageVO> page(MemberPageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 20 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        Long productLineId = parseIdOrNull(dto.getProductLineId(), "产品线 id 非法");

        Page<ProductLineMemberDO> p = new Page<>(pageNumber, pageSize);
        IPage<ProductLineMemberDO> r = memberDao.lambdaQuery()
                .eq(productLineId != null, ProductLineMemberDO::getProductLineId, productLineId)
                .like(dto.getKeyword() != null && !dto.getKeyword().isEmpty(),
                        ProductLineMemberDO::getUserName, dto.getKeyword())
                // 负责人置顶，再按加入时间正序，与运营维护视图直觉一致
                .orderByDesc(ProductLineMemberDO::getIsOwner)
                .orderByAsc(ProductLineMemberDO::getCreateTime)
                .page(p);

        List<ProductLineMemberDO> rows = r.getRecords();
        Map<Long, String> lineNameMap = resolveLineNames(rows);
        List<MemberPageVO> records = rows.stream().map(d -> {
            MemberPageVO v = convert.toPageVO(d);
            v.setProductLineName(lineNameMap.get(d.getProductLineId()));
            return v;
        }).collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberDetailVO detail(Long id) {
        ProductLineMemberDO d = memberDao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "产品线成员不存在");
        }
        MemberDetailVO v = convert.toDetailVO(d);
        ProductLineDO line = productLineDao.getById(d.getProductLineId());
        if (line != null) {
            v.setProductLineName(line.getName());
        }
        // 详情富化工号/部门：成员行只存 user_name，其余展示字段查 sys_user 当前档案
        SysUserDO u = sysUserDao.getById(d.getUserId());
        if (u != null) {
            v.setEmployeeId(u.getEmployeeId());
            v.setDepartmentName(u.getDepartmentName());
        }
        return v;
    }

    @Override
    @Transactional
    public Long add(MemberAddDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        Long productLineId = parseId(dto.getProductLineId(), "产品线 id 非法");
        Long userId = parseId(dto.getUserId(), "用户 id 非法");
        requireProductLineExists(productLineId);
        SysUserDO user = requireActiveUser(userId);

        boolean wantOwner = OWNER == (dto.getIsOwner() == null ? 0 : dto.getIsOwner());
        if (wantOwner) {
            requireNoOtherOwner(productLineId, null);
        }

        ProductLineMemberDO d = convert.toAddDO(dto, productLineId, userId);
        d.setUserName(user.getName());   // 快照取自本地 sys_user 单行（禁 UAA）
        try {
            memberDao.save(d);
        } catch (DuplicateKeyException e) {
            // 命中 uk_plm_line_user：同一产品线重复添加同一人
            throw new BaseException(MemberErrorCode.DUPLICATE_MEMBER, "该用户已是本产品线成员");
        }
        return d.getId();
    }

    @Override
    @Transactional
    public void update(MemberUpdateDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        ProductLineMemberDO existing = memberDao.getById(dto.getId());
        if (existing == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "产品线成员不存在");
        }

        boolean wantOwner = OWNER == (dto.getIsOwner() == null ? 0 : dto.getIsOwner());
        if (wantOwner) {
            // 设 owner 时校验：排除自身后该产品线不能已有其他 owner
            requireNoOtherOwner(existing.getProductLineId(), existing.getId());
        }
        existing.setIsOwner(dto.getIsOwner());
        memberDao.updateById(existing);
    }

    // ---------- private ----------

    /** owner 唯一校验：excludeMemberId 为 null 表示新增场景（不排除任何行）。 */
    private void requireNoOtherOwner(Long productLineId, Long excludeMemberId) {
        boolean exists = memberDao.lambdaQuery()
                .eq(ProductLineMemberDO::getProductLineId, productLineId)
                .eq(ProductLineMemberDO::getIsOwner, OWNER)
                .ne(excludeMemberId != null, ProductLineMemberDO::getId, excludeMemberId)
                .exists();
        if (exists) {
            throw new BaseException(MemberErrorCode.OWNER_CONFLICT,
                    "该产品线已有负责人，请先取消原负责人再设置");
        }
    }

    private void requireProductLineExists(Long productLineId) {
        if (productLineDao.getById(productLineId) == null) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "产品线不存在：" + productLineId);
        }
    }

    private SysUserDO requireActiveUser(Long userId) {
        SysUserDO u = sysUserDao.getById(userId);
        if (u == null || !"active".equals(u.getStatus())) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "目标用户不存在或已停用");
        }
        return u;
    }

    /** 批量解析产品线名（避免逐行联查 N+1）。 */
    private Map<Long, String> resolveLineNames(List<ProductLineMemberDO> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = rows.stream()
                .map(ProductLineMemberDO::getProductLineId)
                .distinct()
                .collect(Collectors.toList());
        return productLineDao.listByIds(ids).stream()
                .collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> a));
    }

    /** 前端 ID 一律 string，非数字直接判非法而非静默丢弃（理由见 OpportunityServiceImpl）。 */
    private Long parseId(String raw, String message) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, message + "：" + raw);
        }
    }

    /** 可空 id：空串/null 返回 null（不过滤），非数字判非法。 */
    private Long parseIdOrNull(String raw, String message) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return parseId(raw, message);
    }
}
