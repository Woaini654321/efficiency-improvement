package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.IdDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.TaskCompleteDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.TaskPageDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.TaskTransferDTO;
import com.quectel.web.cloud.salesleadhubserver.service.TaskService;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import com.quectel.web.cloud.salesleadhubserver.vo.TaskPageVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 「我的任务」接口。路径与前端 {@code apis/task/taskApi.ts} 一一对应。
 *
 * <p>刻意没有 {@code @PreAuthorize}；执行人归属由 service 层校验（当前用户须在该任务
 * assignee 中或为 admin）。</p>
 */
@RestController
@RequestMapping("/task")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageVO<TaskPageVO>> page(@RequestBody TaskPageDTO dto) {
        return Result.success(service.page(dto));
    }

    @PostMapping("/start")
    public Result<Void> start(@Valid @RequestBody IdDTO dto) {
        service.start(dto.getId());
        return Result.success();
    }

    @PostMapping("/complete")
    public Result<Void> complete(@Valid @RequestBody TaskCompleteDTO dto) {
        service.complete(dto.getId(), dto.getRemark());
        return Result.success();
    }

    @PostMapping("/transfer")
    public Result<Void> transfer(@Valid @RequestBody TaskTransferDTO dto) {
        service.transfer(dto);
        return Result.success();
    }
}
