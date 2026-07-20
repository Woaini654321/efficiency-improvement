package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 通知偏好保存入参。
 *
 * <p>契约以前端实际 payload 为准（{@code notification/preference/index.vue} 的
 * {@code saveNotificationPreference({ matrix })}）：matrix 是「通知类型 × 渠道」的开关矩阵，
 * 形如 {@code { publish: { in_app:true, feishu:true, email:false }, ... }}。</p>
 *
 * <p>⚠️ 该端点<b>不写 subscription 表</b>——矩阵是渠道偏好而非分类订阅，前端 payload 里
 * 根本没有 categoryId。偏好整体作为 JSON 快照落 {@code sys_user.notification_preferences} 列。
 * （方案原描述的「写 subscription 全量覆盖」与前端真实契约不符，以前端为准。）</p>
 */
@Data
public class NotificationPreferenceDTO {

    /** 类型→渠道→开关 的三层矩阵，原样存取 */
    @NotNull
    private Map<String, Map<String, Boolean>> matrix;
}
