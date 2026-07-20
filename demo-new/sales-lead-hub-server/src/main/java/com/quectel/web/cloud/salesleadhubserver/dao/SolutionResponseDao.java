package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.SolutionResponseDO;

/**
 * 方案响应（solution_response）DAO。
 *
 * <p>互动模块用它做 {@code target_type=Response} 的目标存在性校验。
 * 注意：solution_response 表<b>没有</b> like_count/collect_count/comment_count 计数列，
 * 故对 Response 的点赞/评论不做计数回写（如后续需要，走按行 COUNT 派生）。</p>
 */
public interface SolutionResponseDao extends IService<SolutionResponseDO> {
}
