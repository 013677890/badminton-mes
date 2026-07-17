package com.badminton.mes.module.barcode.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypePageReqVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeRespVO;
import com.badminton.mes.module.barcode.controller.vo.BarcodeTypeSaveReqVO;
import com.badminton.mes.module.barcode.service.BarcodeTypeService;

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
 * 条码类型 Controller。
 *
 * <p>Web 层职责保持单薄：声明式参数校验、转发 Service、包装统一响应，
 * 不写业务规则(工程结构分层规约)。路径全小写、资源名词用复数(API-001)，
 * 启停用动作子路径 {@code /{id}/enable}、{@code /{id}/disable}。
 *
 * <p>接口按角色限权(拦截器在 token 校验后执行 @RequiresRoles)：
 * 条码配置类写操作限管理员与 PMC(B组与A-C协作边界 4.6 跨组读写边界清单)，
 * 查询接口登录即可。
 *
 * @author 刘涵
 * @date 2026/07/12
 */
@RestController
@RequestMapping("/api/barcode/types")
public class BarcodeTypeController {

    private final BarcodeTypeService barcodeTypeService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param barcodeTypeService 条码类型 Service
     */
    public BarcodeTypeController(BarcodeTypeService barcodeTypeService) {
        this.barcodeTypeService = barcodeTypeService;
    }

    /**
     * 新增条码类型。
     *
     * @param reqVO 创建请求，字段规则见 {@link BarcodeTypeSaveReqVO}
     * @return 新类型主键 id
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createBarcodeType(@Valid @RequestBody BarcodeTypeSaveReqVO reqVO) {
        return CommonResult.success(barcodeTypeService.createBarcodeType(reqVO));
    }

    /**
     * 修改条码类型基础信息。
     *
     * @param id    类型主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateBarcodeType(@PathVariable("id") @Positive Long id,
                                                @Valid @RequestBody BarcodeTypeSaveReqVO reqVO) {
        barcodeTypeService.updateBarcodeType(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 启用条码类型。
     *
     * @param id 类型主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/enable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> enableBarcodeType(@PathVariable("id") @Positive Long id) {
        barcodeTypeService.enableBarcodeType(id);
        return CommonResult.success(null);
    }

    /**
     * 停用条码类型，停用后不允许新建相关应用规则。
     *
     * @param id 类型主键
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/disable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> disableBarcodeType(@PathVariable("id") @Positive Long id) {
        barcodeTypeService.disableBarcodeType(id);
        return CommonResult.success(null);
    }

    /**
     * 删除条码类型(逻辑删除)，已被条码规则或应用规则使用时拒绝。
     *
     * @param id 类型主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteBarcodeType(@PathVariable("id") @Positive Long id) {
        barcodeTypeService.deleteBarcodeType(id);
        return CommonResult.success(null);
    }

    /**
     * 分页查询条码类型。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合(API-002)
     */
    @GetMapping("/page")
    public CommonResult<PageResult<BarcodeTypeRespVO>> getBarcodeTypePage(
            @Valid BarcodeTypePageReqVO reqVO) {
        return CommonResult.success(barcodeTypeService.getBarcodeTypePage(reqVO));
    }

    /**
     * 查询启用条码类型选项，供规则/应用规则配置下拉使用。
     *
     * @return 启用类型列表，无数据时为空集合(API-002)
     */
    @GetMapping("/options")
    public CommonResult<List<BarcodeTypeRespVO>> getEnabledBarcodeTypeOptions() {
        return CommonResult.success(barcodeTypeService.getEnabledBarcodeTypeOptions());
    }

    /**
     * 查询条码类型详情。
     *
     * @param id 类型主键
     * @return 类型详情
     */
    @GetMapping("/{id}")
    public CommonResult<BarcodeTypeRespVO> getBarcodeType(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(barcodeTypeService.getBarcodeType(id));
    }
}
