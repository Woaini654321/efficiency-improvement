package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.convert.MeetingTaskConvert;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingDao;
import com.quectel.web.cloud.salesleadhubserver.dao.MeetingTaskDao;
import com.quectel.web.cloud.salesleadhubserver.dao.SysUserDao;
import com.quectel.web.cloud.salesleadhubserver.dto.BatchPublishDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.BatchTaskDTO;
import com.quectel.web.cloud.salesleadhubserver.exception.MeetingErrorCode;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.service.BatchService;
import com.quectel.web.cloud.salesleadhubserver.service.CurrentUserResolver;
import com.quectel.web.cloud.salesleadhubserver.vo.BatchExecutorVO;
import com.quectel.web.cloud.salesleadhubserver.vo.BatchMeetingVO;
import com.quectel.web.cloud.salesleadhubserver.vo.BatchMetaVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 批量发布业务实现。全部端点仅管理员可用（{@code requireAnyRole(ROLE_ADMIN)}）。
 */
@Service
public class BatchServiceImpl implements BatchService {

    private static final String SOURCE_EXIST = "exist";
    private static final String SOURCE_NEW = "new";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_ACTIVE = "active";

    private final MeetingDao meetingDao;
    private final MeetingTaskDao taskDao;
    private final SysUserDao sysUserDao;
    private final CurrentUserResolver currentUser;
    private final MeetingTaskConvert convert;

    public BatchServiceImpl(MeetingDao meetingDao,
                            MeetingTaskDao taskDao,
                            SysUserDao sysUserDao,
                            CurrentUserResolver currentUser,
                            MeetingTaskConvert convert) {
        this.meetingDao = meetingDao;
        this.taskDao = taskDao;
        this.sysUserDao = sysUserDao;
        this.currentUser = currentUser;
        this.convert = convert;
    }

    @Override
    @Transactional(readOnly = true)
    public BatchMetaVO meta() {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);

        List<BatchMeetingVO> meetings = meetingDao.lambdaQuery()
                .orderByDesc(MeetingDO::getMeetingDate)
                .list().stream()
                .map(convert::toBatchMeetingVO)
                .collect(Collectors.toList());

        List<BatchExecutorVO> executors = sysUserDao.lambdaQuery()
                .eq(SysUserDO::getStatus, STATUS_ACTIVE)
                .list().stream()
                .map(convert::toBatchExecutorVO)
                .collect(Collectors.toList());

        BatchMetaVO vo = new BatchMetaVO();
        vo.setMeetings(meetings);
        vo.setExecutors(executors);
        return vo;
    }

    @Override
    @Transactional
    public void publish(BatchPublishDTO dto) {
        currentUser.requireAnyRole(CurrentUserResolver.ROLE_ADMIN);
        MeetingDO meeting = resolveMeeting(dto);

        for (BatchTaskDTO t : dto.getTasks()) {
            List<Long> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (String eid : t.getExecutorIds()) {
                Long uid = parseUserId(eid);
                SysUserDO u = sysUserDao.getById(uid);
                if (u == null || !STATUS_ACTIVE.equals(u.getStatus())) {
                    throw new BaseException(MeetingErrorCode.EXECUTOR_INVALID,
                            "执行人不存在或已停用：" + eid);
                }
                ids.add(u.getId());
                names.add(u.getName());
            }

            MeetingTaskDO d = new MeetingTaskDO();
            d.setMeetingId(meeting.getId());
            d.setMeetingName(meeting.getName());
            d.setMeetingDate(meeting.getMeetingDate());
            d.setRecorderName(resolveRecorderName(dto, meeting));
            d.setTaskDesc(t.getDesc());
            d.setPriority(t.getPriority());
            d.setDeadline(MeetingTaskConvert.parseDateTime(t.getDeadline()));
            d.setStatus(STATUS_PENDING);
            d.setAssigneeIds(ids);
            d.setAssigneeNames(names);
            d.setTransferFrom("");
            d.setTransferHistory(Collections.emptyList());
            taskDao.save(d);
        }
    }

    // ---------- private ----------

    private MeetingDO resolveMeeting(BatchPublishDTO dto) {
        if (SOURCE_EXIST.equals(dto.getMeetingSource())) {
            if (dto.getMeetingId() == null) {
                throw new BaseException(ErrorCode.PARAM_INVALID, "复用已有会议时 meetingId 必填");
            }
            MeetingDO m = meetingDao.getById(dto.getMeetingId());
            if (m == null) {
                throw new BaseException(MeetingErrorCode.MEETING_NOT_FOUND, "会议不存在");
            }
            return m;
        }
        if (SOURCE_NEW.equals(dto.getMeetingSource())) {
            if (dto.getMeetingName() == null || dto.getMeetingName().trim().isEmpty()) {
                throw new BaseException(ErrorCode.PARAM_INVALID, "新建会议时 meetingName 必填");
            }
            MeetingDO m = new MeetingDO();
            m.setName(dto.getMeetingName());
            m.setMeetingDate(MeetingTaskConvert.parseDateTime(dto.getMeetingDate()));
            m.setRecorderName(dto.getRecorderName());
            meetingDao.save(m);
            return m;
        }
        throw new BaseException(ErrorCode.PARAM_INVALID, "meetingSource 只能是 exist/new：" + dto.getMeetingSource());
    }

    private String resolveRecorderName(BatchPublishDTO dto, MeetingDO meeting) {
        if (dto.getRecorderName() != null && !dto.getRecorderName().trim().isEmpty()) {
            return dto.getRecorderName();
        }
        return meeting.getRecorderName();
    }

    private Long parseUserId(String raw) {
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            throw new BaseException(MeetingErrorCode.EXECUTOR_INVALID, "执行人 id 非法：" + raw);
        }
    }
}
