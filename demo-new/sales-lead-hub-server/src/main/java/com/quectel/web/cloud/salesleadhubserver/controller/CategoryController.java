package com.quectel.web.cloud.salesleadhubserver.controller;

import com.quectel.code.web.pojo.Result;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryActiveDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryCreateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryListDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.CategoryUpdateDTO;
import com.quectel.web.cloud.salesleadhubserver.dto.IdDTO;
import com.quectel.web.cloud.salesleadhubserver.service.CategoryService;
import com.quectel.web.cloud.salesleadhubserver.vo.CategoryVO;
import com.quectel.web.cloud.salesleadhubserver.vo.PageVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 分类维护接口。路径与前端 {@code apis/category/categoryApi.ts} 一一对应。
 * 无 {@code @PreAuthorize}（理由见 RequirementController），写操作在 service 层
 * 要求 admin 角色；list 对全体登录用户开放（发布表单选分类也用它）。
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public Result<PageVO<CategoryVO>> list(@RequestBody CategoryListDTO dto) {
        return Result.success(service.list(dto));
    }

    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody CategoryCreateDTO dto) {
        return Result.success(service.create(dto));
    }

    @PostMapping("/update")
    public Result<Void> update(@Valid @RequestBody CategoryUpdateDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdDTO dto) {
        service.delete(dto.getId());
        return Result.success();
    }

    @PostMapping("/changeActive")
    public Result<Void> changeActive(@Valid @RequestBody CategoryActiveDTO dto) {
        service.changeActive(dto.getId(), Boolean.TRUE.equals(dto.getIsActive()));
        return Result.success();
    }
}
