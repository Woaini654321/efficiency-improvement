package com.quectel.web.cloud.salesleadhubserver.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品线成员分页入参。分页四字段继承 {@link BasePageDTO}。
 *
 * <p>运营「产品线维护」页选定某条产品线后拉取其成员列表，故常规按 productLineId 过滤；
 * 前端 ID 一律 string（Long→String 约定），后端解析为 Long。为空则不按产品线过滤。
 * keyword 模糊匹配成员姓名快照 user_name。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberPageDTO extends BasePageDTO {

    /** 产品线 id（string），可空；非空则只查该产品线的成员 */
    private String productLineId;

    /** 关键词，模糊匹配成员姓名快照 */
    private String keyword;
}
