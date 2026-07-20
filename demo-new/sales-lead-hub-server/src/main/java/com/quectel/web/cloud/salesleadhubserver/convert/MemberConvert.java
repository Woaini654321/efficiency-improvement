package com.quectel.web.cloud.salesleadhubserver.convert;

import com.quectel.web.cloud.salesleadhubserver.dto.MemberAddDTO;
import com.quectel.web.cloud.salesleadhubserver.pojo.ProductLineMemberDO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberPageVO;
import org.springframework.stereotype.Component;

/**
 * 手写 DTO/VO ⇄ DO 映射（不引 MapStruct）。
 *
 * <p>user_name 快照与 create_time 一律不在此处赋值：前者由 service 从本地 sys_user 单行回填
 * （防客户端伪造），后者由框架自动填充。product_line_name/employee_id/department_name 是跨表
 * 展示字段，成员行不存快照，由 service 解析后回填到 VO。</p>
 */
@Component
public class MemberConvert {

    /**
     * 新增 DTO → DO。只映射成员身份与负责人标记；productLineId/userId 由 service 解析 string→Long 后传入。
     * user_name 不在此赋值（service 从 sys_user 回填）。
     */
    public ProductLineMemberDO toAddDO(MemberAddDTO dto, Long productLineId, Long userId) {
        ProductLineMemberDO d = new ProductLineMemberDO();
        d.setProductLineId(productLineId);
        d.setUserId(userId);
        d.setIsOwner(dto.getIsOwner() == null ? 0 : dto.getIsOwner());
        return d;
    }

    /** DO → 列表 VO。product_line_name 由 service 回填（成员行不存该快照）。 */
    public MemberPageVO toPageVO(ProductLineMemberDO d) {
        MemberPageVO v = new MemberPageVO();
        v.setMemberId(d.getId());
        v.setProductLineId(d.getProductLineId());
        v.setUserId(d.getUserId());
        v.setUserName(d.getUserName());
        v.setIsOwner(d.getIsOwner());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }

    /** DO → 详情 VO。product_line_name/employee_id/department_name 由 service 回填。 */
    public MemberDetailVO toDetailVO(ProductLineMemberDO d) {
        MemberDetailVO v = new MemberDetailVO();
        v.setMemberId(d.getId());
        v.setProductLineId(d.getProductLineId());
        v.setUserId(d.getUserId());
        v.setUserName(d.getUserName());
        v.setIsOwner(d.getIsOwner());
        v.setCreatedAt(d.getCreateTime());
        return v;
    }
    // 两个 toXxxVO 逐字段显式赋值、不抽公共基类：契约类型保持扁平可读，
    // 任一 VO 增减字段时编译器能直接指到这里。
}
