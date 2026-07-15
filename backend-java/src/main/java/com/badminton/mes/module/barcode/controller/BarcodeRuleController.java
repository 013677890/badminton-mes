package com.badminton.mes.module.barcode.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRulePreviewRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleSaveReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeRuleValidateRespVO;
import com.badminton.mes.module.barcode.service.BarcodeRuleService;

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

/**
 * 条码规则 Controller。
 *
 * <p>Web 层职责保持单薄：声明式参数校验、转发 Service、包装统一响应。
 * 预览与校验为无副作用计算接口，POST 承载复杂配置请求体。
 *
 * <p>接口按角色限权：条码配置类写操作限管理员与 PMC(B组与A-C协作边界
 * 4.6 跨组读写边界清单)，预览/校验/查询登录即可。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@RestController
@RequestMapping("/api/barcode/rules")
public class BarcodeRuleController {

    private final BarcodeRuleService barcodeRuleService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param barcodeRuleService 条码规则 Service
     */
    public BarcodeRuleController(BarcodeRuleService barcodeRuleService) {
        this.barcodeRuleService = barcodeRuleService;
    }

    /**
     * 新增条码规则及组成明细。
     *
     * @param reqVO 创建请求，字段规则见 {@link BarcodeRuleSaveReqVO}
     * @return 新规则主键 id
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createBarcodeRule(@Valid @RequestBody BarcodeRuleSaveReqVO reqVO) {
        return CommonResult.success(barcodeRuleService.createBarcodeRule(reqVO));
    }

    /**
     * 修改条码规则并整体重写组成明细，只影响新生成条码。
     *
     * @param id    规则主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateBarcodeRule(@PathVariable("id") @Positive Long id,
                                                @Valid @RequestBody BarcodeRuleSaveReqVO reqVO) {
        barcodeRuleService.updateBarcodeRule(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 启用条码规则。
     *
     * @param id 规则主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/enable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> enableBarcodeRule(@PathVariable("id") @Positive Long id) {
        barcodeRuleService.enableBarcodeRule(id);
        return CommonResult.success(null);
    }

    /**
     * 停用条码规则。
     *
     * @param id 规则主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/disable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> disableBarcodeRule(@PathVariable("id") @Positive Long id) {
        barcodeRuleService.disableBarcodeRule(id);
        return CommonResult.success(null);
    }

    /**
     * 删除未使用的条码规则(逻辑删除)。
     *
     * @param id 规则主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteBarcodeRule(@PathVariable("id") @Positive Long id) {
        barcodeRuleService.deleteBarcodeRule(id);
        return CommonResult.success(null);
    }

    /**
     * 预览规则生成结果，不落库、不消耗真实流水。
     *
     * @param reqVO 预览请求，直接携带规则配置
     * @return 预览结果
     */
    @PostMapping("/preview")
    public CommonResult<BarcodeRulePreviewRespVO> previewBarcodeRule(
            @Valid @RequestBody BarcodeRulePreviewReqVO reqVO) {
        return CommonResult.success(barcodeRuleService.previewBarcodeRule(reqVO));
    }

    /**
     * 校验规则配置合法性，返回逐条错误说明。
     *
     * @param reqVO 校验请求
     * @return 校验结果
     */
    @PostMapping("/validate")
    public CommonResult<BarcodeRuleValidateRespVO> validateBarcodeRule(
            @Valid @RequestBody BarcodeRuleValidateReqVO reqVO) {
        return CommonResult.success(barcodeRuleService.validateBarcodeRule(reqVO));
    }

    /**
     * 分页查询条码规则。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<BarcodeRuleRespVO>> getBarcodeRulePage(
            @Valid BarcodeRulePageReqVO reqVO) {
        return CommonResult.success(barcodeRuleService.getBarcodeRulePage(reqVO));
    }

    /**
     * 查询规则详情，含组成明细。
     *
     * @param id 规则主键
     * @return 规则详情
     */
    @GetMapping("/{id}")
    public CommonResult<BarcodeRuleRespVO> getBarcodeRule(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(barcodeRuleService.getBarcodeRule(id));
    }
}
