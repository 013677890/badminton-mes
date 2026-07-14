package com.badminton.mes.module.production.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopPageReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopRespVO;
import com.badminton.mes.module.production.controller.vo.WorkshopSaveReqVO;
import com.badminton.mes.module.production.controller.vo.WorkshopUpdateReqVO;
import com.badminton.mes.module.production.service.WorkshopService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 车间基础资料 Controller。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Validated
@RestController
@RequestMapping("/api/production/workshops")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC,
        RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.CRAFT_ENGINEER})
public class WorkshopController {

    private final WorkshopService workshopService;

    /**
     * 构造车间 Controller。
     *
     * @param workshopService 车间服务
     */
    public WorkshopController(WorkshopService workshopService) {
        this.workshopService = workshopService;
    }

    /**
     * 创建车间。
     *
     * @param reqVO 创建请求
     * @return 新车间主键
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createWorkshop(
            @Valid @RequestBody WorkshopSaveReqVO reqVO) {
        return CommonResult.success(workshopService.createWorkshop(reqVO));
    }

    /**
     * 修改车间。
     *
     * @param id 车间主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateWorkshop(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody WorkshopUpdateReqVO reqVO) {
        workshopService.updateWorkshop(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除无引用车间。
     *
     * @param id 车间主键
     * @param version 客户端预期版本
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteWorkshop(
            @PathVariable("id") @Positive Long id,
            @RequestParam("version") @PositiveOrZero Integer version) {
        workshopService.deleteWorkshop(id, version);
        return CommonResult.success(null);
    }

    /**
     * 启用或停用车间。
     *
     * @param id 车间主键
     * @param reqVO 状态变更请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateWorkshopStatus(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ProductionStatusReqVO reqVO) {
        workshopService.updateWorkshopStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 查询车间详情。
     *
     * @param id 车间主键
     * @return 车间详情
     */
    @GetMapping("/{id}")
    public CommonResult<WorkshopRespVO> getWorkshop(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(workshopService.getWorkshop(id));
    }

    /**
     * 分页查询车间。
     *
     * @param reqVO 分页筛选条件
     * @return 车间分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<WorkshopRespVO>> getWorkshopPage(
            @Valid WorkshopPageReqVO reqVO) {
        return CommonResult.success(workshopService.getWorkshopPage(reqVO));
    }
}
