package com.badminton.mes.module.barcode.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleOptionReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRulePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeApplicationRuleSaveReqVO;
import com.badminton.mes.module.barcode.service.BarcodeApplicationRuleService;

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
 * 条码应用规则 Controller。
 *
 * <p>应用规则把产品/物料、条码类型、条码规则和标签模板组合成条码生成入口。
 * 接口按角色限权：配置类写操作限管理员与 PMC，查询与选项登录即可。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@RestController
@RequestMapping("/api/barcode/application_rules")
public class BarcodeApplicationRuleController {

    private final BarcodeApplicationRuleService barcodeApplicationRuleService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param barcodeApplicationRuleService 条码应用规则 Service
     */
    public BarcodeApplicationRuleController(BarcodeApplicationRuleService barcodeApplicationRuleService) {
        this.barcodeApplicationRuleService = barcodeApplicationRuleService;
    }

    /**
     * 新增条码应用规则。
     *
     * @param reqVO 创建请求，字段规则见 {@link BarcodeApplicationRuleSaveReqVO}
     * @return 新应用规则主键 id
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createBarcodeApplicationRule(
            @Valid @RequestBody BarcodeApplicationRuleSaveReqVO reqVO) {
        return CommonResult.success(barcodeApplicationRuleService.createBarcodeApplicationRule(reqVO));
    }

    /**
     * 修改条码应用规则。
     *
     * @param id    应用规则主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateBarcodeApplicationRule(@PathVariable("id") @Positive Long id,
            @Valid @RequestBody BarcodeApplicationRuleSaveReqVO reqVO) {
        barcodeApplicationRuleService.updateBarcodeApplicationRule(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 启用应用规则，启用前校验条码类型、条码规则和标签模板均处于启用状态。
     *
     * @param id 应用规则主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/enable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> enableBarcodeApplicationRule(@PathVariable("id") @Positive Long id) {
        barcodeApplicationRuleService.enableBarcodeApplicationRule(id);
        return CommonResult.success(null);
    }

    /**
     * 停用应用规则。
     *
     * @param id 应用规则主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/disable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> disableBarcodeApplicationRule(@PathVariable("id") @Positive Long id) {
        barcodeApplicationRuleService.disableBarcodeApplicationRule(id);
        return CommonResult.success(null);
    }

    /**
     * 删除未使用的应用规则(逻辑删除)。
     *
     * @param id 应用规则主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteBarcodeApplicationRule(@PathVariable("id") @Positive Long id) {
        barcodeApplicationRuleService.deleteBarcodeApplicationRule(id);
        return CommonResult.success(null);
    }

    /**
     * 分页查询应用规则。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<BarcodeApplicationRuleRespVO>> getBarcodeApplicationRulePage(
            @Valid BarcodeApplicationRulePageReqVO reqVO) {
        return CommonResult.success(barcodeApplicationRuleService.getBarcodeApplicationRulePage(reqVO));
    }

    /**
     * 查询生成条码时可用的启用应用规则选项，默认规则在前。
     *
     * @param reqVO 选项过滤条件，GET 查询参数绑定
     * @return 启用应用规则列表，无数据时为空集合(API-002)
     */
    @GetMapping("/options")
    public CommonResult<List<BarcodeApplicationRuleRespVO>> getBarcodeApplicationRuleOptions(
            @Valid BarcodeApplicationRuleOptionReqVO reqVO) {
        return CommonResult.success(barcodeApplicationRuleService.getBarcodeApplicationRuleOptions(reqVO));
    }

    /**
     * 查询应用规则详情。
     *
     * @param id 应用规则主键
     * @return 应用规则详情
     */
    @GetMapping("/{id}")
    public CommonResult<BarcodeApplicationRuleRespVO> getBarcodeApplicationRule(
            @PathVariable("id") @Positive Long id) {
        return CommonResult.success(barcodeApplicationRuleService.getBarcodeApplicationRule(id));
    }
}
