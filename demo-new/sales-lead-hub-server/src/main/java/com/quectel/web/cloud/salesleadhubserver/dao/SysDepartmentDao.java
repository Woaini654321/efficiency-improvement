package com.quectel.web.cloud.salesleadhubserver.dao;

import com.baomidou.mybatisplus.extension.service.IService;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysDepartmentDO;

import java.util.List;

/**
 * 部门（sys_department）数据访问。
 *
 * <p>SLA L2 升级人（部门负责人）由本表 {@code owner_name} 快照派生：发布人 department_id →
 * 部门 owner。只读消费，无写入端点。</p>
 */
public interface SysDepartmentDao extends IService<SysDepartmentDO> {

    /**
     * 查启用中的部门（{@code is_active=1}），按 sort_order 升序。供部门树选择器组树使用。
     *
     * <p>把 is_active 过滤 + 排序封装成 Dao 方法（而非在 service 裸写 lambdaQuery），
     * 便于 service 单测直接 mock，无需 mock MyBatis-Plus 链式调用。全局按 sort_order 升序，
     * 同一父下的子节点相对顺序即为其 sort_order 顺序。</p>
     */
    List<SysDepartmentDO> listActiveOrderBySort();
}
