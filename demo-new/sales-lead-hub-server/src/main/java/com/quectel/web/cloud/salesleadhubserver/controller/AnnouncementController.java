package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.AnnouncementPageDTO;
import com.quectel.web.cloud.salesleadhubserver.service.AnnouncementService;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.AnnouncementPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台公告接口。路径与前端 {@code apis/notification/announcementApi.ts} 一一对应
 * （{@code announcement/page}、{@code announcement/detail}）。
 *
 * <p><b>刻意没有 {@code @PreAuthorize}</b>：登录态由框架 {@code TokenValidationFilter}
 * 统一保证；前台端登录即可访问，不校业务角色（sales/product_manager/admin 均可看已发布公告）。
 * 业务角色为何不用 UAA 判定，见 {@code CurrentUserResolver}。可见性（只出 published）由
 * service 强制过滤，不依赖角色。</p>
 *
 * <p>与 {@link OperationAnnounceController} 分成两个类：前台/运营是两套 VO 与两套读语义，
 * 挤一个类会让"前台只出已发布"与"运营全量含草稿"的边界模糊。</p>
 */
@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    private final AnnouncementService service;

    public AnnouncementController(AnnouncementService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<AnnouncementPageVO>> page(@RequestBody AnnouncementPageDTO dto) {
        return Result.success(service.frontPage(dto));
    }

    @GetMapping("/detail")
    public Result<AnnouncementDetailVO> detail(@RequestParam("id") Long id) {
        return Result.success(service.frontDetail(id));
    }
}
