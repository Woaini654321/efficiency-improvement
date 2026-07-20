package com.quectel.web.cloud.salesleadhubserver.vo;

import lombok.Data;

import java.util.List;

/**
 * 批量发布向导元数据（operation/batch/meta）出参 {@code {meetings, executors}}。
 *
 * <p>顶层两个 key 本身就是小写单词，无需 @JsonNaming；嵌套项各自带类级 snake 策略
 * （见 {@link BatchMeetingVO} / {@link BatchExecutorVO}）。</p>
 */
@Data
public class BatchMetaVO {

    private List<BatchMeetingVO> meetings;

    private List<BatchExecutorVO> executors;
}
