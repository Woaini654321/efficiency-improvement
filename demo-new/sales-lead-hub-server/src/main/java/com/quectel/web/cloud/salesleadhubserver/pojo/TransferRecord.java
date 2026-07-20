package com.quectel.web.cloud.salesleadhubserver.pojo;

import lombok.Data;

import java.io.Serializable;

/**
 * 任务转交记录（JSON 列内嵌结构，非独立表）。
 *
 * <p>契约 {@code {time,from,to,reason}} 与前端 {@code TaskTransferRecord}（task/types.ts）
 * 逐字一致；字段全是单词小写，snake_case 序列化是 no-op，可直接用于 VO 出参。</p>
 *
 * <p>存放于 {@code meeting_task.transfer_history}（JSON 数组），每转交一次追加一条。</p>
 */
@Data
public class TransferRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 转交时间（yyyy-MM-dd HH:mm:ss） */
    private String time;

    /** 转出人姓名 */
    private String from;

    /** 转入人姓名 */
    private String to;

    /** 转交原因 */
    private String reason;
}
