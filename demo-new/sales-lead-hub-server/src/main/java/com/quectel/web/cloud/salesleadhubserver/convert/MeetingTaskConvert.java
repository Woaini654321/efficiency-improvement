package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.code.web.exception.BaseException;
import com.quectel.code.web.exception.ErrorCode;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MeetingUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.MeetingTaskDO;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysUserDO;
import com.quectel.web.cloud.salesleadhubserver.vo.BatchExecutorVO;
import com.quectel.web.cloud.salesleadhubserver.vo.BatchMeetingVO;
import com.quectel.web.cloud.salesleadhubserver.vo.MeetingTaskPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.TaskPageVO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 手写 DTO/VO ⇄ DO 映射（meeting/task/batch 共用）。
 *
 * <p>审计字段（createTime/createBy/updateTime/updateBy）一律不在此赋值，由框架 {@code SecurityMetaObjectHandler}
 * 填充；会议名/时间/记录人等快照列由 service 按「复用/新建会议」结果回填。</p>
 */
@Component
public class MeetingTaskConvert {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_NO_SEC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 前端日期字符串 → LocalDateTime。兼容三种前端 valueFormat：
     * 纯日期 {@code yyyy-MM-dd}（补 00:00:00）、{@code yyyy-MM-dd HH:mm}、{@code yyyy-MM-dd HH:mm:ss}。
     * 空串/null 返回 null；格式非法抛 PARAM_INVALID（不静默吞）。
     */
    public static LocalDateTime parseDateTime(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            if (s.length() <= 10) {
                return LocalDate.parse(s, DATE).atStartOfDay();
            }
            if (s.length() == 16) {
                return LocalDateTime.parse(s, DATE_TIME_NO_SEC);
            }
            return LocalDateTime.parse(s, DATE_TIME);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.PARAM_INVALID, "日期格式非法：" + raw);
        }
    }

    /**
     * 创建任务：仅映射任务本体字段与截止时间；会议快照（meetingName/meetingDate/recorderName）
     * 与 meetingId、assigneeIds、status 由 service 按会议复用/新建结果回填。
     */
    public MeetingTaskDO toCreateDO(MeetingCreateDTO dto) {
        MeetingTaskDO d = new MeetingTaskDO();
        d.setTaskDesc(dto.getTaskDesc());
        d.setPriority(dto.getPriority());
        d.setDeadline(parseDateTime(dto.getDeadline()));
        d.setAssigneeNames(dto.getAssigneeNames());
        return d;
    }

    /**
     * 更新任务：改任务字段 + 会议快照列（编辑抽屉里会议名/时间/记录人一并可改）。
     * 状态、转交历史、执行人 id 不在此动。
     */
    public void applyUpdate(MeetingUpdateDTO dto, MeetingTaskDO d) {
        d.setMeetingName(dto.getMeetingName());
        d.setMeetingDate(parseDateTime(dto.getMeetingDate()));
        d.setRecorderName(dto.getRecorderName());
        d.setTaskDesc(dto.getTaskDesc());
        d.setPriority(dto.getPriority());
        d.setDeadline(parseDateTime(dto.getDeadline()));
        d.setAssigneeNames(dto.getAssigneeNames());
    }

    public MeetingTaskPageVO toPageVO(MeetingTaskDO d) {
        MeetingTaskPageVO v = new MeetingTaskPageVO();
        v.setTaskId(d.getId());
        v.setMeetingName(d.getMeetingName());
        v.setMeetingDate(d.getMeetingDate());
        v.setRecorderName(d.getRecorderName());
        v.setTaskDesc(d.getTaskDesc());
        v.setPriority(d.getPriority());
        v.setDeadline(d.getDeadline());
        v.setAssigneeNames(d.getAssigneeNames());
        v.setStatus(d.getStatus());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    public TaskPageVO toTaskVO(MeetingTaskDO d) {
        TaskPageVO v = new TaskPageVO();
        v.setTaskId(d.getId());
        v.setMeetingName(d.getMeetingName());
        v.setMeetingDate(d.getMeetingDate());
        v.setPriority(d.getPriority());
        v.setStatus(d.getStatus());
        v.setDeadline(d.getDeadline());
        v.setTaskDesc(d.getTaskDesc());
        v.setRecorderName(d.getRecorderName());
        // 前端 adapter 用 ?? '' 兜底，这里 null 归一为空串，语义「非转交」
        v.setTransferFrom(d.getTransferFrom() == null ? "" : d.getTransferFrom());
        v.setAssigneeNames(d.getAssigneeNames());
        v.setTransferHistory(d.getTransferHistory());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    public BatchMeetingVO toBatchMeetingVO(MeetingDO m) {
        BatchMeetingVO v = new BatchMeetingVO();
        v.setMeetingId(m.getId());
        v.setName(m.getName());
        v.setMeetingDate(m.getMeetingDate());
        v.setRecorderName(m.getRecorderName());
        return v;
    }

    public BatchExecutorVO toBatchExecutorVO(SysUserDO u) {
        BatchExecutorVO v = new BatchExecutorVO();
        v.setUserId(u.getId());
        v.setName(u.getName());
        v.setDeptName(u.getDepartmentName());
        return v;
    }
    // 各 toXxxVO 逐字段显式赋值、不抽公共基类：契约类型保持扁平可读，
    // 任一 VO 增减字段时编译器能直接指到这里。
}
