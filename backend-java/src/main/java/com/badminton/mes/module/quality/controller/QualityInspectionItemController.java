package com.badminton.mes.module.quality.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemPageReqVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemRespVO;
import com.badminton.mes.module.quality.controller.vo.QualityInspectionItemSaveReqVO;
import com.badminton.mes.module.quality.service.QualityInspectionItemService;

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

/** 质量检验项目接口。 */
@RestController
@RequestMapping("/api/quality/inspection-items")
public class QualityInspectionItemController {

    private final QualityInspectionItemService itemService;

    public QualityInspectionItemController(QualityInspectionItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody QualityInspectionItemSaveReqVO request) {
        return CommonResult.success(itemService.createItem(request));
    }

    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionItemSaveReqVO request) {
        itemService.updateItem(id, request);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        itemService.deleteItem(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionItemRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(itemService.getItem(id));
    }

    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionItemRespVO>> page(
            @Valid QualityInspectionItemPageReqVO request) {
        return CommonResult.success(itemService.getItemPage(request));
    }
}
