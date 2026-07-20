package com.quectel.web.cloud.salesleadhubserver.service;

import com.quectel.web.cloud.salesleadhubserver.dto.MemberAddDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;

/** 产品线成员业务。 */
public interface MemberService {

    PageVO<MemberPageVO> page(MemberPageDTO dto);

    MemberDetailVO detail(Long id);

    /** 新增成员：admin 限定；owner 唯一校验；user_name 快照取自本地 sys_user。返回成员行 id。 */
    Long add(MemberAddDTO dto);

    /** 更新成员负责人标记：admin 限定；设 owner 时校验该产品线唯一 owner。 */
    void update(MemberUpdateDTO dto);
}
