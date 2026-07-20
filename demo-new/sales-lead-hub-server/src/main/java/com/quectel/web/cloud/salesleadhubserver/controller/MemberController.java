package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberAddDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.MemberUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.service.MemberService;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.MemberPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 产品线成员接口。路径与前端 {@code apis/MemberApi.ts} 的 url 字面量一一对应：
 * {@code member/page}（POST 分页）、{@code member}（GET 详情，GETURL 追加 id 为路径段）、
 * {@code member/add}（POST 新增）、{@code member/update}（PUT 更新负责人标记）。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无本平台业务角色，理由见 CurrentUserResolver）；
 * 业务角色由 service 层 {@code CurrentUserResolver} 查本地 sys_user.role 判定——写操作限 admin，
 * 读操作放开给登录用户。</p>
 */
@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService service;

    public MemberController(MemberService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<MemberPageVO>> page(@RequestBody MemberPageDTO dto) {
        return Result.success(service.page(dto));
    }

    /** 前端 {@code request.GETURL({url:'member'}, id)} → GET /member/{id}。 */
    @GetMapping("/{id}")
    public Result<MemberDetailVO> detail(@PathVariable("id") Long id) {
        return Result.success(service.detail(id));
    }

    @PostMapping("/add")
    public Result<Long> add(@Valid @RequestBody MemberAddDTO dto) {
        return Result.success(service.add(dto));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody MemberUpdateDTO dto) {
        service.update(dto);
        return Result.success();
    }
}
