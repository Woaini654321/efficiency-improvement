package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CompetitorIntelSubmitDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.IndustryIntelPageDTO;
import com.quectel.web.cloud.salesleadhubserver.service.IntelService;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.CompetitorIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelDetailVO;
import com.quectel.web.cloud.salesleadhubserver.vo.IndustryIntelPageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 情报中心接口。路径与前端 {@code apis/intel/intelApi.ts} 一一对应
 * （intel/competitor/{page,detail,submit} + intel/industry/{page,detail}）。
 *
 * <p>刻意没有 {@code @PreAuthorize}（UAA 无业务角色，理由见 RequirementController）；
 * 登录门槛由 service 层判定（登录即可，无 admin 门槛）。</p>
 *
 * <p>page 端点刻意不加 {@code @Valid}：前端全量拉取实发 pageSize=999，交由 service 的
 * {@code Math.min(500)} 夹逼，而非让 @Max(500) 校验直接 400（同 OpportunityController.page）。</p>
 */
@RestController
@RequestMapping("/intel")
public class IntelController {

    private final IntelService service;

    public IntelController(IntelService service) {
        this.service = service;
    }

    @PostMapping("/competitor/page")
    public Result<PageVO<CompetitorIntelPageVO>> competitorPage(@RequestBody CompetitorIntelPageDTO dto) {
        return Result.success(service.competitorPage(dto));
    }

    @GetMapping("/competitor/detail")
    public Result<CompetitorIntelDetailVO> competitorDetail(@RequestParam("id") Long id) {
        return Result.success(service.competitorDetail(id));
    }

    @PostMapping("/competitor/submit")
    public Result<Long> submitCompetitor(@Valid @RequestBody CompetitorIntelSubmitDTO dto) {
        return Result.success(service.submitCompetitor(dto));
    }

    @PostMapping("/industry/page")
    public Result<PageVO<IndustryIntelPageVO>> industryPage(@RequestBody IndustryIntelPageDTO dto) {
        return Result.success(service.industryPage(dto));
    }

    @GetMapping("/industry/detail")
    public Result<IndustryIntelDetailVO> industryDetail(@RequestParam("id") Long id) {
        return Result.success(service.industryDetail(id));
    }
}
