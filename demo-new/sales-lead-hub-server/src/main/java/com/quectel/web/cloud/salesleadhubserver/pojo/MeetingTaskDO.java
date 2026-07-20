package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议任务（meeting_task）。一行 = 一条任务，是 meeting/task/batch 三模块的共同落库表。
 *
 * <p>会议名/时间/记录人以<b>快照列</b>冗余存储（免 JOIN 直接出列表）。含三个 JSON 列
 * （assignee_ids/assignee_names/transfer_history），故类级 {@code autoResultMap = true}
 * 且各 JSON 列显式 {@code typeHandler = JacksonTypeHandler.class}（写法照抄 {@link OpportunityDO}）。</p>
 *
 * <p>schema 无逻辑删除、无乐观锁列，故继承 {@link BaseEntity} 但不带 {@code deleted}/{@code version}。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName(value = "meeting_task", autoResultMap = true)
public class MeetingTaskDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 会议 FK */
    private Long meetingId;

    /** 会议名快照 */
    private String meetingName;

    /** 会议时间快照 */
    private LocalDateTime meetingDate;

    /** 记录人姓名快照 */
    private String recorderName;

    /** 任务描述 */
    private String taskDesc;

    /** normal / urgent / critical */
    private String priority;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** pending / processing / completed / transferred / cancelled */
    private String status;

    /** 执行人 id 集（JSON 数组；会议单填姓名的过渡态可为空） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> assigneeIds;

    /** 执行人姓名快照集（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> assigneeNames;

    /** 转出人姓名（空=非转交） */
    private String transferFrom;

    /** 转交历史 [{time,from,to,reason}]（JSON 数组） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<TransferRecord> transferHistory;

    /** 完成备注 */
    private String completeRemark;

    /** 取消原因 */
    private String cancelReason;

    /** 最近催办时间（通知推送后续接入） */
    private LocalDateTime lastUrgedAt;
}
