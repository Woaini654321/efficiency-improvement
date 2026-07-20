package com.quectel.web.cloud.salesleadhubserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quectel.web.cloud.salesleadhubserver.dao.SysDepartmentDao;
import com.quectel.web.cloud.salesleadhubserver.pojo.SysDepartmentDO;
import com.quectel.web.cloud.salesleadhubserver.service.impl.DepartmentServiceImpl;
import com.quectel.web.cloud.salesleadhubserver.vo.DeptNodeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部门树 service 测试：两级组树 + 孤儿挂根策略 + 空集；外加 VO 离线 snake_case 序列化门禁。
 * dao 全 mock，离线可跑。
 */
class DepartmentServiceImplTest {

    @Mock SysDepartmentDao deptDao;

    private DepartmentServiceImpl service;
    private AutoCloseable mocks;

    @BeforeEach
    void init() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new DepartmentServiceImpl(deptDao);
    }

    @AfterEach
    void close() throws Exception {
        mocks.close();
    }

    private SysDepartmentDO dept(Long id, Long parentId, String name) {
        SysDepartmentDO d = new SysDepartmentDO();
        d.setId(id);
        d.setParentId(parentId);
        d.setName(name);
        return d;
    }

    private DeptNodeVO findRoot(List<DeptNodeVO> roots, Long id) {
        return roots.stream().filter(n -> id.equals(n.getDepartmentId())).findFirst().orElse(null);
    }

    @Test
    void tree_builds_two_levels_and_promotes_orphan_to_root() {
        // 10 华东大区(根) → 1001 上海销售组(子)；30 产品管理部(根)；
        // 2001 深圳销售组 父 999 不在集合（父被停用/悬空）→ 孤儿挂根
        SysDepartmentDO root1 = dept(10L, null, "华东大区");
        SysDepartmentDO child = dept(1001L, 10L, "上海销售组");
        SysDepartmentDO root2 = dept(30L, null, "产品管理部");
        SysDepartmentDO orphan = dept(2001L, 999L, "深圳销售组");
        when(deptDao.listActiveOrderBySort())
                .thenReturn(Arrays.asList(root1, child, root2, orphan));

        List<DeptNodeVO> tree = service.tree();

        // service 走的是 active-only 查询
        verify(deptDao).listActiveOrderBySort();
        // 根：10、30、孤儿 2001 → 3 个
        assertEquals(3, tree.size());
        // 10 下挂 1001
        DeptNodeVO n10 = findRoot(tree, 10L);
        assertEquals(1, n10.getChildren().size());
        assertEquals(Long.valueOf(1001L), n10.getChildren().get(0).getDepartmentId());
        // 30 无子节点 → 空数组
        assertEquals(0, findRoot(tree, 30L).getChildren().size());
        // 孤儿提升为根而非丢弃
        assertTrue(findRoot(tree, 2001L) != null, "孤儿部门应提升为根节点");
    }

    @Test
    void tree_empty_returns_empty_list() {
        when(deptDao.listActiveOrderBySort()).thenReturn(Collections.emptyList());
        assertTrue(service.tree().isEmpty());
    }

    @Test
    void vo_serializes_snake_case_and_no_camel_case() throws Exception {
        DeptNodeVO parent = new DeptNodeVO();
        parent.setDepartmentId(123456789012345678L);
        parent.setName("华东大区");
        DeptNodeVO kid = new DeptNodeVO();
        kid.setDepartmentId(1001L);
        kid.setName("上海销售组");
        parent.getChildren().add(kid);

        String json = new ObjectMapper().writeValueAsString(parent);

        assertTrue(json.contains("\"department_id\""), json);
        assertTrue(json.contains("\"name\""), json);
        assertTrue(json.contains("\"children\""), json);
        assertFalse(json.contains("departmentId"), "不得出现 camelCase：" + json);
    }
}
