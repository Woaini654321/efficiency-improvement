package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.IntelConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.CompetitorIntelDao;
import com.quectel.web.cloud.salesleadhubserver.dao.IndustryIntelDao;
import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelSubmitDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.IndustryIntelPageDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.IntelErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.CompetitorIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.IndustryIntelDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.IntelService;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 情报中心 service。
 *
 * <p>全模块无 admin 门槛：登录即可读写（运营治理下期）。读接口不显式校角色
 * （TokenValidationFilter 已保证已登录）；submit 调 {@code requireAnyRole(全部三角色)}
 * 仅为 fail-closed 拿到本地档案回填 submitter 快照。</p>
 */
@Service
public class IntelServiceImpl implements IntelService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼（前端实发 pageSize=999）。 */
    private static final int MAX_PAGE_SIZE = 500;

    private final CompetitorIntelDao competitorDao;
    private final IndustryIntelDao industryDao;
    private final CurrentUserResolver currentUser;
    private final IntelConvert convert;

    public IntelServiceImpl(CompetitorIntelDao competitorDao,
                            IndustryIntelDao industryDao,
                            CurrentUserResolver currentUser,
                            IntelConvert convert) {
        this.competitorDao = competitorDao;
        this.industryDao = industryDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    // ---------- 竞品情报 ----------

    @Override
    @Transactional(readOnly = true)
    public PageVO<CompetitorIntelPageVO> competitorPage(CompetitorIntelPageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 20 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        String kw = dto.getKeyword();

        Page<CompetitorIntelDO> p = new Page<>(pageNumber, pageSize);
        IPage<CompetitorIntelDO> r = competitorDao.lambdaQuery()
                .eq(dto.getBrand() != null && !dto.getBrand().isEmpty(),
                        CompetitorIntelDO::getBrand, dto.getBrand())
                .eq(dto.getIntelType() != null && !dto.getIntelType().isEmpty(),
                        CompetitorIntelDO::getIntelType, dto.getIntelType())
                // keyword 模糊匹配 title 或 summary
                .and(kw != null && !kw.isEmpty(), w -> w
                        .like(CompetitorIntelDO::getTitle, kw)
                        .or().like(CompetitorIntelDO::getSummary, kw))
                .orderByDesc(CompetitorIntelDO::getCreateTime)
                .page(p);

        List<CompetitorIntelPageVO> records = r.getRecords().stream()
                .map(convert::toCompetitorPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public CompetitorIntelDetailVO competitorDetail(Long id) {
        CompetitorIntelDO d = competitorDao.getById(id);
        if (d == null) {
            throw new BaseException(IntelErrorCode.INTEL_NOT_FOUND, "竞品情报不存在");
        }
        // 浏览数原子自增（服务器侧），返回值上反映 +1 免二次查询
        competitorDao.increaseViewCount(id);
        d.setViewCount((d.getViewCount() == null ? 0 : d.getViewCount()) + 1);
        return convert.toCompetitorDetailVO(d);
    }

    @Override
    @Transactional
    public Long submitCompetitor(CompetitorIntelSubmitDTO dto) {
        // 登录即可（fail-closed 拿本地档案回填 submitter 快照，不取 UAA）
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);

        CompetitorIntelDO d = convert.toSubmitDO(dto);
        d.setSubmitterId(me.getId());
        d.setSubmitterName(me.getName());
        d.setLikeCount(0);
        d.setCollectCount(0);
        d.setViewCount(0);

        competitorDao.save(d);
        return d.getId();
    }

    // ---------- 行业情报 ----------

    @Override
    @Transactional(readOnly = true)
    public PageVO<IndustryIntelPageVO> industryPage(IndustryIntelPageDTO dto) {
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 20 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        String kw = dto.getKeyword();

        Page<IndustryIntelDO> p = new Page<>(pageNumber, pageSize);
        IPage<IndustryIntelDO> r = industryDao.lambdaQuery()
                .eq(dto.getIndustry() != null && !dto.getIndustry().isEmpty(),
                        IndustryIntelDO::getIndustry, dto.getIndustry())
                .and(kw != null && !kw.isEmpty(), w -> w
                        .like(IndustryIntelDO::getTitle, kw)
                        .or().like(IndustryIntelDO::getSummary, kw))
                .orderByDesc(IndustryIntelDO::getCreateTime)
                .page(p);

        List<IndustryIntelPageVO> records = r.getRecords().stream()
                .map(convert::toIndustryPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public IndustryIntelDetailVO industryDetail(Long id) {
        IndustryIntelDO d = industryDao.getById(id);
        if (d == null) {
            throw new BaseException(IntelErrorCode.INTEL_NOT_FOUND, "行业情报不存在");
        }
        industryDao.increaseViewCount(id);
        d.setViewCount((d.getViewCount() == null ? 0 : d.getViewCount()) + 1);
        return convert.toIndustryDetailVO(d);
    }
}
