package com.badminton.mes.module.quality.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategoryRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionCategorySaveReqVO;
import com.badminton.mes.module.quality.service.QualityInspectionCategoryService;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import static com.badminton.mes.common.security.RoleCodeConstants.ADMIN;
import static com.badminton.mes.common.security.RoleCodeConstants.INSPECTOR;
import static com.badminton.mes.common.security.RoleCodeConstants.PMC;
import static com.badminton.mes.common.security.RoleCodeConstants.TEAM_LEADER;
import static com.badminton.mes.common.security.RoleCodeConstants.WORKSHOP_MANAGER;

/** 质量检验分类接口。 */
@RestController
@RequestMapping("/api/quality/inspection-categories")
public class QualityInspectionCategoryController {

    private final QualityInspectionCategoryService categoryService;

    public QualityInspectionCategoryController(QualityInspectionCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody QualityInspectionCategorySaveReqVO request) {
        return CommonResult.success(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionCategorySaveReqVO request) {
        categoryService.updateCategory(id, request);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        categoryService.deleteCategory(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionCategoryRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(categoryService.getCategory(id));
    }

    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionCategoryRespVO>> page(
            @Valid QualityInspectionCategoryPageReqVO request) {
        return CommonResult.success(categoryService.getCategoryPage(request));
    }
}
