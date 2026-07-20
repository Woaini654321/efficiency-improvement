package com.quectel.web.cloud.salesleadhubserver.service.impl;

import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysDepartmentDO;
import com.quectel.web.cloud.salesleadhubserver.service.DepartmentService;
import com.quectel.web.cloud.salesleadhubserver.vo.DeptNodeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门树业务实现。
 *
 * <p>登录即可查（无角色门槛，与 Employee/ProductLine 选择器同为只读选择器，供需求可见性的
 * dept 选择器使用）；已登录由 SSO TokenValidationFilter 保证，故 service 层不做角色校验。</p>
 *
 * <p>只取 is_active=1 的部门（过滤在 dao 的 WHERE 内），一次性全量查出后<b>内存组树</b>
 * （parent_id 自引用），组装逻辑就地放在 service（端点简单，不单列 Convert）。</p>
 */
@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final SysDepartmentDao deptDao;

    public DepartmentServiceImpl(SysDepartmentDao deptDao) {
        this.deptDao = deptDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeptNodeVO> tree() {
        return buildTree(deptDao.listActiveOrderBySort());
    }

    /**
     * 把扁平部门行按 parent_id 自引用组装成树。
     *
     * <p><b>孤儿挂根策略</b>：parent_id 为 null（本就是根），或其父部门不在本集合里
     * （父被停用 is_active=0、或数据悬空）时，把该部门<b>提升为根节点</b>展示，而非静默丢弃
     * 其整棵子树——部门选择器需覆盖所有启用部门，丢失比错位更不可接受（与 DiscussionConvert
     * 组树的孤儿处理一致）。同级顺序沿用入参顺序（dao 已按 sort_order 升序，故同一父下的
     * 子节点即按 sort_order 排列）。</p>
     */
    private List<DeptNodeVO> buildTree(List<SysDepartmentDO> rows) {
        List<DeptNodeVO> roots = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return roots;
        }
        // 建索引，保留 sort_order 入参顺序
        Map<Long, DeptNodeVO> index = new LinkedHashMap<>();
        for (SysDepartmentDO row : rows) {
            index.put(row.getId(), toNode(row));
        }
        // 二次遍历挂树
        for (SysDepartmentDO row : rows) {
            DeptNodeVO node = index.get(row.getId());
            Long parentId = row.getParentId();
            DeptNodeVO parent = parentId == null ? null : index.get(parentId);
            if (parent == null) {
                // 根，或父不在集合（父被停用/悬空）的孤儿 → 挂根
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    private static DeptNodeVO toNode(SysDepartmentDO d) {
        DeptNodeVO v = new DeptNodeVO();
        v.setDepartmentId(d.getId());
        v.setName(d.getName());
        return v;
    }
}
