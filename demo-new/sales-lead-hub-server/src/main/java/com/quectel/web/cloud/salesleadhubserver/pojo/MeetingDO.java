package com.quectel.web.cloud.salesleadhubserver.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 会议（meeting）。会议主体，一次会议可拆出多条 {@link MeetingTaskDO}。
 *
 * <p>schema.sql「Phase 6 补充表」定义：仅含 4 审计列，无逻辑删除、无乐观锁，故继承
 * {@link BaseEntity} 但不带 {@code deleted}/{@code version}。审计列由框架
 * {@code SecurityMetaObjectHandler} 自动填充，业务禁手动赋值。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TableName("meeting")
public class MeetingDO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 会议名 */
    private String name;

    /** 会议时间 */
    private LocalDateTime meetingDate;

    /** 记录人 FK（自由填写姓名时可为空） */
    private Long recorderId;

    /** 记录人姓名快照 */
    private String recorderName;
}
