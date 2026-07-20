package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.MeetingTaskConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.TaskPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.TaskTransferDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.MeetingErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.TransferRecord;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.service.TaskService;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.TaskPageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 「我的任务」业务实现。
 *
 * <p>三个操作端点都要求当前用户在该任务 assignee 中（或 admin）——
 * {@link #requireAssigneeOrAdmin}。状态机非法跃迁抛
 * {@link MeetingErrorCode#ILLEGAL_TRANSITION}。</p>
 */
@Service
public class TaskServiceImpl implements TaskService {

    private static final int MAX_PAGE_SIZE = 500;

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_TRANSFERRED = "transferred";

    private static final DateTimeFormatter NOW_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MeetingTaskDao taskDao;
    private final SysUserDao sysUserDao;
    private final CurrentUserResolver currentUser;
    private final MeetingTaskConvert convert;

    public TaskServiceImpl(MeetingTaskDao taskDao,
                           SysUserDao sysUserDao,
                           CurrentUserResolver currentUser,
                           MeetingTaskConvert convert) {
        this.taskDao = taskDao;
        this.sysUserDao = sysUserDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<TaskPageVO> page(TaskPageDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        int pageNumber = dto.getPageNumber() == null ? 1 : dto.getPageNumber();
        int pageSize = dto.getPageSize() == null ? 10 : Math.min(dto.getPageSize(), MAX_PAGE_SIZE);

        Long uid = me.getId();
        String myName = me.getName();

        Page<MeetingTaskDO> p = new Page<>(pageNumber, pageSize);
        IPage<MeetingTaskDO> r = taskDao.lambdaQuery()
                .eq(dto.getStatus() != null && !dto.getStatus().isEmpty(),
                        MeetingTaskDO::getStatus, dto.getStatus())
                .eq(dto.getPriority() != null && !dto.getPriority().isEmpty(),
                        MeetingTaskDO::getPriority, dto.getPriority())
                // 「我的任务」：assignee_ids 含我的 id，或 assignee_names 含我的姓名
                // （兼容会议单填姓名、无 id 的过渡态），两条件 OR
                .and(w -> w
                        .apply("JSON_CONTAINS(assignee_ids, {0})", String.valueOf(uid))
                        .or()
                        .apply("JSON_CONTAINS(assignee_names, JSON_QUOTE({0}))", myName))
                .orderByDesc(MeetingTaskDO::getCreateTime)
                .page(p);

        List<TaskPageVO> records = r.getRecords().stream()
                .map(convert::toTaskVO)
                .collect(Collectors.toList());
        return new PageVO<>(records, r.getTotal());
    }

    @Override
    @Transactional
    public void start(Long id) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        MeetingTaskDO d = loadTask(id);
        requireAssigneeOrAdmin(d, me);
        if (!STATUS_PENDING.equals(d.getStatus())) {
            throw new BaseException(MeetingErrorCode.ILLEGAL_TRANSITION, "仅待处理任务可开始处理");
        }
        d.setStatus(STATUS_PROCESSING);
        taskDao.updateById(d);
    }

    @Override
    @Transactional
    public void complete(Long id, String remark) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        MeetingTaskDO d = loadTask(id);
        requireAssigneeOrAdmin(d, me);
        if (!STATUS_PENDING.equals(d.getStatus()) && !STATUS_PROCESSING.equals(d.getStatus())) {
            throw new BaseException(MeetingErrorCode.ILLEGAL_TRANSITION, "仅待处理/处理中的任务可完成");
        }
        d.setStatus(STATUS_COMPLETED);
        d.setCompleteRemark(remark);
        taskDao.updateById(d);
    }

    @Override
    @Transactional
    public void transfer(TaskTransferDTO dto) {
        SysUserDO me = currentUser.requireAnyRole(
                CurrentUserResolver.ROLE_SALES,
                CurrentUserResolver.ROLE_PRODUCT_MANAGER,
                CurrentUserResolver.ROLE_ADMIN);
        MeetingTaskDO d = loadTask(dto.getId());
        requireAssigneeOrAdmin(d, me);
        if (STATUS_COMPLETED.equals(d.getStatus())) {
            throw new BaseException(MeetingErrorCode.ILLEGAL_TRANSITION, "已完成的任务不可转交");
        }

        // transferTo 兼容两种取值：纯数字按 sys_user id 解析取姓名，否则直接当姓名用
        String raw = dto.getTransferTo().trim();
        Long targetId = null;
        String targetName = raw;
        if (raw.matches("\\d+")) {
            SysUserDO target = sysUserDao.getById(Long.valueOf(raw));
            if (target == null || !"active".equals(target.getStatus())) {
                throw new BaseException(MeetingErrorCode.EXECUTOR_INVALID, "转交目标用户不存在或已停用");
            }
            targetId = target.getId();
            targetName = target.getName();
        }

        List<TransferRecord> history = d.getTransferHistory() == null
                ? new ArrayList<>() : new ArrayList<>(d.getTransferHistory());
        TransferRecord rec = new TransferRecord();
        rec.setTime(LocalDateTime.now().format(NOW_FMT));
        rec.setFrom(me.getName());
        rec.setTo(targetName);
        rec.setReason(dto.getReason());
        history.add(rec);

        d.setTransferHistory(history);
        d.setTransferFrom(me.getName());
        d.setStatus(STATUS_TRANSFERRED);
        // 替换语义：执行人换成目标人
        d.setAssigneeNames(Collections.singletonList(targetName));
        d.setAssigneeIds(targetId == null ? Collections.emptyList() : Collections.singletonList(targetId));
        taskDao.updateById(d);
    }

    // ---------- private ----------

    private MeetingTaskDO loadTask(Long id) {
        MeetingTaskDO d = taskDao.getById(id);
        if (d == null) {
            throw new BaseException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return d;
    }

    /** 当前用户须是该任务执行人（id 或姓名命中）或管理员，否则拒绝。 */
    private void requireAssigneeOrAdmin(MeetingTaskDO d, SysUserDO me) {
        if (CurrentUserResolver.ROLE_ADMIN.equals(me.getRole())) {
            return;
        }
        boolean byId = d.getAssigneeIds() != null && d.getAssigneeIds().contains(me.getId());
        boolean byName = d.getAssigneeNames() != null && d.getAssigneeNames().contains(me.getName());
        if (!byId && !byName) {
            throw new BaseException(MeetingErrorCode.NOT_ASSIGNEE, "只能操作指派给自己的任务");
        }
    }
}
