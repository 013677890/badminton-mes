package com.badminton.mes.module.production.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductionLinePageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineRespVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionLineUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.service.ProductionLineService;

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
 * 产线基础资料 Controller。
 *
 * @author 张竹灏
 * @date 2026/07/12
 */
@Validated
@RestController
@RequestMapping("/api/production/production_lines")
public class ProductionLineController {

    private final ProductionLineService productionLineService;

    /**
     * 构造产线 Controller。
     *
     * @param productionLineService 产线服务
     */
    public ProductionLineController(ProductionLineService productionLineService) {
        this.productionLineService = productionLineService;
    }

    /**
     * 创建产线。
     *
     * @param reqVO 创建请求
     * @return 新产线主键
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createProductionLine(
            @Valid @RequestBody ProductionLineSaveReqVO reqVO) {
        return CommonResult.success(
                productionLineService.createProductionLine(reqVO));
    }

    /**
     * 修改产线。
     *
     * @param id 产线主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateProductionLine(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ProductionLineUpdateReqVO reqVO) {
        productionLineService.updateProductionLine(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除无引用产线。
     *
     * @param id 产线主键
     * @param version 客户端预期版本
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteProductionLine(
            @PathVariable("id") @Positive Long id,
            @RequestParam("version") @PositiveOrZero Integer version) {
        productionLineService.deleteProductionLine(id, version);
        return CommonResult.success(null);
    }

    /**
     * 启用或停用产线。
     *
     * @param id 产线主键
     * @param reqVO 状态变更请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateProductionLineStatus(
            @PathVariable("id") @Positive Long id,
            @Valid @RequestBody ProductionStatusReqVO reqVO) {
        productionLineService.updateProductionLineStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 查询产线详情。
     *
     * @param id 产线主键
     * @return 产线详情
     */
    @GetMapping("/{id}")
    public CommonResult<ProductionLineRespVO> getProductionLine(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(productionLineService.getProductionLine(id));
    }

    /**
     * 分页查询产线。
     *
     * @param reqVO 分页筛选条件
     * @return 产线分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<ProductionLineRespVO>> getProductionLinePage(
            @Valid ProductionLinePageReqVO reqVO) {
        return CommonResult.success(
                productionLineService.getProductionLinePage(reqVO));
    }
}
