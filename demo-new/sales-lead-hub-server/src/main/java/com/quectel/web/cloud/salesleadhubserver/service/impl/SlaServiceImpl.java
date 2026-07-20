package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.quectel.code.web.exception.BaseException;
import com.quectel.web.cloud.salesleadhubserver.convert.SlaConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.ProductLineMemberDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequestProductLineDao;
import com.quectel.web.cloud.salesleadhubserver.dao.RequirementDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dao.NotificationDao;
import com.quectel.web.cloud.salesleadhubserver.dto.SlaPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.SlaUrgeDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.SlaErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.NotificationDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.OpportunityRequestDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.RequestProductLineDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysDepartmentDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.SlaCalculator;
import com.quectel.web.cloud.salesleadhubserver.service.SlaService;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaEmailContactVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaMetaVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaProductLeadVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaRequestVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaStatsVO;
import com.quectel.web.cloud.salesleadhubserver.vo.SlaTimelineVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SLA 时效监控只读聚合实现。全部端点入口 {@code requireAnyRole(ROLE_ADMIN)} fail-closed。
 *
 * <p>派生逻辑委托 {@link SlaCalculator}（纯函数、可离线单测）；本类只负责取数、
 * 组装展示（时间线 notify 名称）、内存过滤与分页。数据量小，一律 {@code list} 直查后
 * Stream 现算，不写复杂 JOIN（与迁移方案 Phase 5 约束一致）。</p>
 */
@Service
public class SlaServiceImpl implements SlaService {

    /** 框架分页拦截器 maxLimit，超出静默截断，故显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final DateTimeFormatter TL_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final RequirementDao requirementDao;
    private final ProductLineMemberDao productLineMemberDao;
    private final ProductLineDao productLineDao;
    private final RequestProductLineDao requestProductLineDao;
    private final SysDepartmentDao sysDepartmentDao;
    private final SysUserDao sysUserDao;
    private final NotificationDao notificationDao;
    private final CurrentUserResolver currentUser;
    private final SlaCalculator calculator;
    private final SlaConvert convert;

    public SlaServiceImpl(RequirementDao requirementDao,
                          ProductLineMemberDao productLineMemberDao,
                          ProductLineDao productLineDao,
                          RequestProductLineDao requestProductLineDao,
                          SysDepartmentDao sysDepartmentDao,
                          SysUserDao sysUserDao,
                          NotificationDao notificationDao,
                          CurrentUserResolver currentUser,
                          SlaCalculator calculator,
                          SlaConvert convert) {
        this.requirementDao = requirementDao;
        this.productLineMemberDao = productLineMemberDao;
        this.productLineDao = productLineDao;
        this.requestProductLineDao = requestProductLineDao;
        this.sysDepartmentDao = sysDepartmentDao;
        this.sysUserDao = sysUserDao;
        this.notificationDao = notificationDao;
        this.currentUser = currentUser;
        this.calculator = calculator;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<SlaRequestVO> page(SlaPageDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        LocalDateTime now = LocalDateTime.now();

        // DB 侧只做 urgency + 日期硬过滤；slaStatus 是实时派生值，只能内存过滤
        List<OpportunityRequestDO> all = requirementDao.lambdaQuery()
                .eq(dto.getUrgency() != null, OpportunityRequestDO::getUrgency, dto.getUrgency())
                .ge(parseStart(dto.getStartDate()) != null, OpportunityRequestDO::getCreateTime, parseStart(dto.getStartDate()))
                .le(parseEnd(dto.getEndDate()) != null, OpportunityRequestDO::getCreateTime, parseEnd(dto.getEndDate()))
                .orderByDesc(OpportunityRequestDO::getCreateTime)
                .list();

        // 按实时派生状态过滤
        List<OpportunityRequestDO> filtered = all.stream()
                .filter(d -> dto.getSlaStatus() == null
                        || dto.getSlaStatus().equals(calculator.deriveStatus(d, now)))
                .collect(Collectors.toList());

        long total = filtered.size();

        // 内存分页（前端实际拉全量本地分页，这里仍按入参切片保证契约一致）
        int pageNumber = dto.getPageNumber() == null ? 1 : Math.max(1, dto.getPageNumber());
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        int from = Math.min((pageNumber - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        List<OpportunityRequestDO> slice = filtered.subList(from, to);

        // 时间线 notify 名称所需的三张映射，整页只加载一次（避免逐行 N+1）
        NotifyMaps maps = loadNotifyMaps();

        List<SlaRequestVO> records = slice.stream()
                .map(d -> toVO(d, now, maps))
                .collect(Collectors.toList());
        return new PageVO<>(records, total);
    }

    @Override
    @Transactional(readOnly = true)
    public SlaStatsVO stats() {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        LocalDateTime now = LocalDateTime.now();

        List<OpportunityRequestDO> all = requirementDao.list();
        int total = all.size();
        int responded = 0;
        int overdue = 0;
        long maxOverMinutes = 0L;
        for (OpportunityRequestDO d : all) {
            if (calculator.isResponded(d)) {
                responded++;
                continue;
            }
            if (SlaCalculator.STATUS_OVERDUE.equals(calculator.deriveStatus(d, now))) {
                overdue++;
                LocalDateTime deadline = calculator.deadline(d);
                if (deadline != null) {
                    long over = java.time.Duration.between(deadline, now).toMinutes();
                    if (over > maxOverMinutes) {
                        maxOverMinutes = over;
                    }
                }
            }
        }

        SlaStatsVO vo = new SlaStatsVO();
        vo.setTotalRequests(total);
        // 及时率 = 未超时占比（已响应 + 未超时未响应）；除 0 防护
        double timely = total == 0 ? 0d : (total - overdue) * 100.0 / total;
        vo.setTimelyRate(Math.round(timely * 10) / 10.0);
        vo.setRespondedCount(responded);
        vo.setMaxOverdueText(maxOverMinutes <= 0 ? "" : Math.round(maxOverMinutes / 60.0) + "h");
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public SlaMetaVO meta() {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        // 产品线负责人候选：product_line_member(is_owner=1) join product_line；dept 取负责人本地档案
        List<ProductLineMemberDO> owners = productLineMemberDao.lambdaQuery()
                .eq(ProductLineMemberDO::getIsOwner, 1)
                .list();
        Map<Long, String> lineNames = productLineDao.list().stream()
                .collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (a, b) -> a));
        Map<Long, SysUserDO> users = owners.isEmpty() ? Collections.<Long, SysUserDO>emptyMap()
                : sysUserDao.listByIds(owners.stream().map(ProductLineMemberDO::getUserId)
                        .collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(SysUserDO::getId, Function.identity(), (a, b) -> a));

        List<SlaProductLeadVO> leads = owners.stream().map(m -> {
            SlaProductLeadVO v = new SlaProductLeadVO();
            v.setId(m.getUserId());
            SysUserDO u = users.get(m.getUserId());
            v.setName(m.getUserName() != null ? m.getUserName() : (u != null ? u.getName() : ""));
            v.setProduct(lineNames.getOrDefault(m.getProductLineId(), ""));
            v.setDept(u != null && u.getDepartmentName() != null ? u.getDepartmentName() : "");
            return v;
        }).collect(Collectors.toList());

        // 邮件通知人候选：admin 角色且有邮箱的用户
        List<SlaEmailContactVO> contacts = sysUserDao.lambdaQuery()
                .eq(SysUserDO::getRole, CurrentUserResolver.ROLE_ADMIN)
                .isNotNull(SysUserDO::getEmail)
                .list().stream().map(u -> {
                    SlaEmailContactVO c = new SlaEmailContactVO();
                    c.setLabel((u.getName() == null ? u.getUsername() : u.getName()) + " — " + u.getEmail());
                    c.setValue(u.getEmail());
                    return c;
                }).collect(Collectors.toList());

        SlaMetaVO vo = new SlaMetaVO();
        vo.setProductLeads(leads);
        vo.setEmailContacts(contacts);
        return vo;
    }

    @Override
    @Transactional
    public void urge(SlaUrgeDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        Long reqId;
        try {
            reqId = Long.valueOf(dto.getId());
        } catch (NumberFormatException e) {
            throw new BaseException(SlaErrorCode.REQUEST_NOT_FOUND, "催办目标需求 id 非法：" + dto.getId());
        }
        OpportunityRequestDO req = requirementDao.getById(reqId);
        if (req == null) {
            throw new BaseException(SlaErrorCode.REQUEST_NOT_FOUND, "催办目标需求不存在");
        }

        Set<Long> targetUserIds = resolveTargetUsers(dto.getTargets(), req);

        boolean inApp = dto.getMethods() != null && dto.getMethods().contains("in_app");
        // feishu/email 渠道当前无消息中台落地：仅 in_app 写 notification 行，
        // feishu/email 待接入企业消息中台后补（javadoc 记录，不静默假成功）。
        if (inApp) {
            if (targetUserIds.isEmpty()) {
                throw new BaseException(SlaErrorCode.NO_VALID_TARGET, "未能解析出任何本地接收人（发布人/部门负责人/产品线负责人均缺失）");
            }
            // 幂等窗：同一需求 10 分钟内已有 sla_remind 通知则跳过重复插入，
            // 防连点/重复催办把发布人收件箱刷屏。窗口内视为已催办，直接返回不报错。
            boolean recentlyReminded = notificationDao.lambdaQuery()
                    .eq(NotificationDO::getType, "sla_remind")
                    .eq(NotificationDO::getTargetType, "Request")
                    .eq(NotificationDO::getTargetId, reqId)
                    .ge(NotificationDO::getCreateTime, LocalDateTime.now().minusMinutes(10))
                    .exists();
            if (recentlyReminded) {
                return;
            }
            List<NotificationDO> rows = targetUserIds.stream().map(uid -> {
                NotificationDO n = new NotificationDO();
                n.setUserId(uid);
                n.setTargetType("Request");
                n.setTargetId(reqId);
                n.setType("sla_remind");
                n.setChannel("in_app");
                n.setTitle("需求首响催办：" + req.getTitle());
                n.setTriggerUserName(me.getName());
                n.setIsRead(0);
                n.setIsForceConfirm(0);
                return n;
            }).collect(Collectors.toList());
            notificationDao.saveBatch(rows);
        }
        // dto.remark 当前 notification 表无对应列，暂不落库（后续如需催办备注需加列）。
    }

    // ---------- private ----------

    /** targets 类别 → 本地 sys_user id 集（去重、保序）。 */
    private Set<Long> resolveTargetUsers(List<String> targets, OpportunityRequestDO req) {
        Set<Long> ids = new LinkedHashSet<>();
        if (targets == null) {
            return ids;
        }
        for (String t : targets) {
            if ("publisher".equals(t)) {
                if (req.getPublisherId() != null) {
                    ids.add(req.getPublisherId());
                }
            } else if ("supervisor".equals(t)) {
                if (req.getDepartmentId() != null) {
                    SysDepartmentDO dept = sysDepartmentDao.getById(req.getDepartmentId());
                    if (dept != null && dept.getOwnerId() != null) {
                        ids.add(dept.getOwnerId());
                    }
                }
            } else if ("product_lead".equals(t)) {
                ids.addAll(productLeadUserIds(req.getId()));
            }
        }
        return ids;
    }

    /** 需求邀请产品线的负责人 sys_user id 集。 */
    private List<Long> productLeadUserIds(Long requestId) {
        List<Long> lineIds = requestProductLineDao.lambdaQuery()
                .eq(RequestProductLineDO::getRequestId, requestId)
                .list().stream().map(RequestProductLineDO::getProductLineId).collect(Collectors.toList());
        if (lineIds.isEmpty()) {
            return Collections.emptyList();
        }
        return productLineMemberDao.lambdaQuery()
                .eq(ProductLineMemberDO::getIsOwner, 1)
                .in(ProductLineMemberDO::getProductLineId, lineIds)
                .list().stream().map(ProductLineMemberDO::getUserId).collect(Collectors.toList());
    }

    /** 整页共用的三张 notify 名称映射，一次性加载。 */
    private NotifyMaps loadNotifyMaps() {
        NotifyMaps m = new NotifyMaps();
        // 产品线负责人姓名（productLineId -> 姓名集）
        Map<Long, List<String>> ownerNamesByLine = productLineMemberDao.lambdaQuery()
                .eq(ProductLineMemberDO::getIsOwner, 1)
                .list().stream()
                .collect(Collectors.groupingBy(ProductLineMemberDO::getProductLineId,
                        Collectors.mapping(o -> o.getUserName() == null ? "" : o.getUserName(), Collectors.toList())));
        m.ownerNamesByLine = ownerNamesByLine;
        // 需求邀请产品线（requestId -> productLineId 集）
        m.linesByRequest = requestProductLineDao.list().stream()
                .collect(Collectors.groupingBy(RequestProductLineDO::getRequestId,
                        Collectors.mapping(RequestProductLineDO::getProductLineId, Collectors.toList())));
        // 部门负责人姓名（deptId -> ownerName）
        m.deptOwnerName = sysDepartmentDao.list().stream()
                .filter(d -> d.getOwnerName() != null)
                .collect(Collectors.toMap(SysDepartmentDO::getId, SysDepartmentDO::getOwnerName, (a, b) -> a));
        return m;
    }

    /** DO + 实时派生 + 时间线 → VO。 */
    private SlaRequestVO toVO(OpportunityRequestDO d, LocalDateTime now, NotifyMaps maps) {
        String status = calculator.deriveStatus(d, now);
        String level = calculator.deriveEscalationLevel(d, now);
        String remaining = calculator.deriveRemainingText(d, now);
        LocalDateTime deadline = calculator.deadline(d);
        List<SlaTimelineVO> timeline = buildTimeline(d, now, status, maps);
        return convert.toRequestVO(d, status, level, remaining, deadline, timeline);
    }

    /** 按已达状态/级数生成升级时间线（纯展示）。 */
    private List<SlaTimelineVO> buildTimeline(OpportunityRequestDO d, LocalDateTime now,
                                              String status, NotifyMaps maps) {
        List<SlaTimelineVO> tl = new ArrayList<>();
        String pub = d.getPublisherName() == null ? "" : d.getPublisherName();
        tl.add(convert.timelineRow(fmt(d.getCreateTime()), "需求发布，计时开始", "发布人 " + pub));

        if (calculator.isResponded(d)) {
            tl.add(convert.timelineRow(fmt(d.getUpdateTime()), "已收到首个方案响应，计时终止", "-"));
            return tl;
        }
        if (SlaCalculator.STATUS_WARNING.equals(status)) {
            tl.add(convert.timelineRow(fmt(calculator.deadline(d)), "临近首响时限，系统预警", "发布人 " + pub));
            return tl;
        }
        if (SlaCalculator.STATUS_OVERDUE.equals(status)) {
            int levelInt = calculator.escalationLevelInt(d, now);
            int t = calculator.thresholdMinutes(d.getUrgency());
            for (int i = 1; i <= levelInt; i++) {
                LocalDateTime escTime = d.getCreateTime() == null ? null
                        : d.getCreateTime().plusMinutes((long) i * t);
                String notify = escalationNotify(i, d, maps);
                tl.add(convert.timelineRow(fmt(escTime),
                        "已超过首响时限，第" + i + "级升级提醒（L" + i + "）",
                        "发布人 " + pub + "、" + notify));
            }
        }
        return tl;
    }

    /** L1=产品线负责人、L2=部门负责人、L3=平台运营；取不到给占位说明。 */
    private String escalationNotify(int level, OpportunityRequestDO d, NotifyMaps maps) {
        if (level == 1) {
            List<Long> lines = maps.linesByRequest.getOrDefault(d.getId(), Collections.emptyList());
            List<String> names = lines.stream()
                    .flatMap(lid -> maps.ownerNamesByLine.getOrDefault(lid, Collections.emptyList()).stream())
                    .filter(s -> !s.isEmpty())
                    .distinct().collect(Collectors.toList());
            return names.isEmpty() ? "产品线负责人（未配置）" : "产品线负责人 " + String.join("、", names);
        }
        if (level == 2) {
            String owner = d.getDepartmentId() == null ? null : maps.deptOwnerName.get(d.getDepartmentId());
            return owner == null ? "部门负责人（未配置）" : "部门负责人 " + owner;
        }
        return "平台运营";
    }

    private String fmt(LocalDateTime t) {
        return t == null ? "" : t.format(TL_FMT);
    }

    private LocalDateTime parseStart(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private LocalDateTime parseEnd(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atTime(23, 59, 59);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 整页共用的三张 notify 名称映射容器。 */
    private static final class NotifyMaps {
        Map<Long, List<String>> ownerNamesByLine = Collections.emptyMap();
        Map<Long, List<Long>> linesByRequest = Collections.emptyMap();
        Map<Long, String> deptOwnerName = Collections.emptyMap();
    }
}
