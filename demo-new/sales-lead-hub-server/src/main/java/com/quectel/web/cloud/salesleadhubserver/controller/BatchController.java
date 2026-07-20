package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.BatchPublishDTO;
import com.quectel.web.cloud.salesleadhubserver.service.BatchService;
import com.quectel.web.cloud.salesleadhubserver.vo.BatchMetaVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 批量发布接口。路径与前端 {@code apis/batch/batchApi.ts} 一一对应，注意带
 * {@code operation/} 前缀（{@code operation/batch/meta}、{@code operation/batch/publish}）。
 *
 * <p>刻意没有 {@code @PreAuthorize}；全部端点仅 admin 可用，由 service 层
 * {@code requireAnyRole(ROLE_ADMIN)} 判定。</p>
 */
@RestController
@RequestMapping("/operation/batch")
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @GetMapping("/meta")
    public Result<BatchMetaVO> meta() {
        return Result.success(service.meta());
    }

    @PostMapping("/publish")
    public Result<Void> publish(@Valid @RequestBody BatchPublishDTO dto) {
        service.publish(dto);
        return Result.success();
    }
}
