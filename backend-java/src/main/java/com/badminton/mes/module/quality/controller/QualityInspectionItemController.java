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

/**
 * 质量检验项目基础资料接口。
 *
 * <p>检验项目定义数值、文本或布尔值的采集方式及自动/人工判定规则；管理员和检验员负责维护，
 * 计划、车间及班组岗位可读取项目以展示方案和检验要求。
 */
@RestController
@RequestMapping("/api/quality/inspection-items")
public class QualityInspectionItemController {

    private final QualityInspectionItemService itemService;

    public QualityInspectionItemController(QualityInspectionItemService itemService) {
        this.itemService = itemService;
    }

    /** 创建检验项目基础资料，并返回新项目主键。 */
    @PostMapping
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Long> create(@Valid @RequestBody QualityInspectionItemSaveReqVO request) {
        return CommonResult.success(itemService.createItem(request));
    }

    /** 修改指定检验项目；项目主键必须为正整数，关联分类及判定配置由请求校验。 */
    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody QualityInspectionItemSaveReqVO request) {
        itemService.updateItem(id, request);
        return CommonResult.success(null);
    }

    /** 删除指定检验项目；被方案引用等不可删除情形由服务层进行业务校验。 */
    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        itemService.deleteItem(id);
        return CommonResult.success(null);
    }

    /** 按正整数主键查询检验项目及其分类、判定规则和启用状态。 */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<QualityInspectionItemRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(itemService.getItem(id));
    }

    /** 按关键字、分类、值类型、必检标记和启用状态分页筛选检验项目。 */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, INSPECTOR, PMC, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<PageResult<QualityInspectionItemRespVO>> page(
            @Valid QualityInspectionItemPageReqVO request) {
        return CommonResult.success(itemService.getItemPage(request));
    }
}
