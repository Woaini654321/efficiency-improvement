package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.web.cloud.salesleadhubserver.convert.AuditLogConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.AuditLogDao;
import com.quectel.web.cloud.salesleadhubserver.dto.AuditLogPageDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.AuditLogDO;
import com.quectel.web.cloud.salesleadhubserver.service.AuditLogService;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.vo.AuditLogPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MAX_PAGE_SIZE = 500;

    private final AuditLogDao dao;
    private final CurrentUserResolver currentUser;
    private final AuditLogConvert convert;

    public AuditLogServiceImpl(AuditLogDao dao,
                               CurrentUserResolver currentUser,
                               AuditLogConvert convert) {
        this.dao = dao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<AuditLogPageVO> page(AuditLogPageDTO dto) {
        // 审计日志含 IP/UA/操作轨迹，只有运营管理员可看
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 20 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);

        boolean hasKeyword = dto.getKeyword() != null && !dto.getKeyword().isEmpty();
        Page<AuditLogDO> p = new Page<>(pageNumber, pageSize);
        IPage<AuditLogDO> r = dao.lambdaQuery()
                .and(hasKeyword, w -> w.like(AuditLogDO::getTarget, dto.getKeyword())
                        .or().like(AuditLogDO::getOperatorName, dto.getKeyword()))
                .eq(dto.getActionType() != null && !dto.getActionType().isEmpty(),
                        AuditLogDO::getActionType, dto.getActionType())
                .eq(dto.getResult() != null && !dto.getResult().isEmpty(),
                        AuditLogDO::getResult, dto.getResult())
                .orderByDesc(AuditLogDO::getCreateTime)
                .page(p);

        List<AuditLogPageVO> records = r.getRecords().stream()
                .map(convert::toVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }
}
