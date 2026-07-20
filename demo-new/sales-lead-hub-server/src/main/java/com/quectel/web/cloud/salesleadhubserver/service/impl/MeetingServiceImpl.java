package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.MeetingTaskConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.MeetingErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.MeetingService;
import com.quectel.web.cloud.salesleadhubserver.vo.MeetingTaskPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会议任务管理业务实现。
 *
 * <p>写方法开头一律 {@code requireAnyRole(...)}；update/urge/cancel 再校验
 * admin 或该任务 create_by 本人（{@link #requireOwnerOrAdmin}）。</p>
 */
@Service
public class MeetingServiceImpl implements MeetingService {

    /** 框架分页拦截器的 maxLimit，超出会被静默截断，故在此显式夹逼。 */
    private static final int MAX_PAGE_SIZE = 500;

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_CANCELLED = "cancelled";

    private final MeetingDao meetingDao;
    private final MeetingTaskDao taskDao;
    private final CurrentUserResolver currentUser;
    private final MeetingTaskConvert convert;

    public MeetingServiceImpl(MeetingDao meetingDao,
                              MeetingTaskDao taskDao,
                              CurrentUserResolver currentUser,
                              MeetingTaskConvert convert) {
        this.meetingDao = meetingDao;
        this.taskDao = taskDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<MeetingTaskPageVO> page(MeetingPageDTO dto) {
        // 会议任务是管理视图，登录且具备任一业务角色即可查看
        currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);
        String kw = dto.getKeyword();

        Page<MeetingTaskDO> p = new Page<>(pageNumber, pageSize);
        IPage<MeetingTaskDO> r = taskDao.lambdaQuery()
                .and(kw != null && !kw.isEmpty(), w -> w
                        .like(MeetingTaskDO::getMeetingName, kw)
                        .or().like(MeetingTaskDO::getTaskDesc, kw))
                .eq(dto.getStatus() != null && !dto.getStatus().isEmpty(),
                        MeetingTaskDO::getStatus, dto.getStatus())
                .eq(dto.getPriority() != null && !dto.getPriority().isEmpty(),
                        MeetingTaskDO::getPriority, dto.getPriority())
                .orderByDesc(MeetingTaskDO::getCreateTime)
                .page(p);

        List<MeetingTaskPageVO> records = r.getRecords().stream()
                .map(convert::toPageVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public Long create(MeetingCreateDTO dto) {
        // 创建端点：具备任一业务角色即可（新任务尚无 owner，无从校验本人）
        currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);

        LocalDateTime meetingDate = MeetingTaskConvert.parseDateTime(dto.getMeetingDate());
        MeetingDO meeting = findOrCreateMeeting(dto.getMeetingName(), meetingDate, dto.getRecorderName());

        MeetingTaskDO d = convert.toCreateDO(dto);
        d.setMeetingId(meeting.getId());
        d.setMeetingName(meeting.getName());
        d.setMeetingDate(meeting.getMeetingDate());
        d.setRecorderName(dto.getRecorderName());
        d.setStatus(STATUS_PENDING);
        // 会议单填姓名，无执行人 id；任务视图按姓名匹配（过渡态）
        d.setAssigneeIds(Collections.emptyList());
        d.setTransferHistory(Collections.emptyList());
        d.setTransferFrom("");
        taskDao.save(d);
        return d.getId();
    }

    @Override
    @Transactional
    public void update(MeetingUpdateDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        MeetingTaskDO d = taskDao.getById(dto.getId());
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "会议任务不存在");
        }
        requireOwnerOrAdmin(d, me, "只能编辑自己创建的会议任务");
        convert.applyUpdate(dto, d);
        taskDao.updateById(d);
    }

    @Override
    @Transactional
    public void urge(Long id, String remark) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        MeetingTaskDO d = taskDao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "会议任务不存在");
        }
        requireOwnerOrAdmin(d, me, "只能催办自己创建的会议任务");
        // 通知推送后续接入，这里仅记录催办时间戳
        d.setLastUrgedAt(LocalDateTime.now());
        taskDao.updateById(d);
    }

    @Override
    @Transactional
    public void cancel(Long id, String reason) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        MeetingTaskDO d = taskDao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "会议任务不存在");
        }
        requireOwnerOrAdmin(d, me, "只能作废自己创建的会议任务");
        if (STATUS_COMPLETED.equals(d.getStatus())) {
            throw new BaseException(MeetingErrorCode.ILLEGAL_TRANSITION, "已完成的任务不可作废");
        }
        d.setStatus(STATUS_CANCELLED);
        d.setCancelReason(reason);
        taskDao.updateById(d);
    }

    // ---------- private ----------

    /** 按 (会议名, 会议时间) 查会议，命中则复用、否则新建。 */
    private MeetingDO findOrCreateMeeting(String name, LocalDateTime meetingDate, String recorderName) {
        // 用 getOne(Wrapper) 而非 lambdaQuery() 链式，便于 service 层单测 mock
        LambdaQueryWrapper<MeetingDO> w = new LambdaQueryWrapper<>();
        w.eq(MeetingDO::getName, name);
        w.eq(meetingDate != null, MeetingDO::getMeetingDate, meetingDate);
        w.last("limit 1");
        MeetingDO existing = meetingDao.getOne(w);
        if (existing != null) {
            return existing;
        }
        MeetingDO m = new MeetingDO();
        m.setName(name);
        m.setMeetingDate(meetingDate);
        m.setRecorderName(recorderName);
        meetingDao.save(m);
        return m;
    }

    private void requireOwnerOrAdmin(MeetingTaskDO d, SysUserDO me, String message) {
        boolean isOwner = d.getCreateBy() != null && d.getCreateBy().equals(me.getId());
        if (!isOwner && !CurrentUserResolver.ROLE_ADMIN.equals(me.getRole())) {
            throw new BaseException(MeetingErrorCode.NOT_OWNER, message);
        }
    }
}
