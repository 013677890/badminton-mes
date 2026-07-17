package com.badminton.mes.module.production.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.vo.ProductPageReqVO;
import com.badminton.mes.module.production.controller.vo.ProductRespVO;
import com.badminton.mes.module.production.controller.vo.ProductSaveReqVO;
import com.badminton.mes.module.production.controller.vo.ProductUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.service.ProductService;

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
 * 产品主档 Controller。
 *
 * <p>本层不执行主档引用检查或数据库写入，只将校验后的请求转交 Service，并统一包装返回结果。
 */
@RestController
@RequestMapping("/api/production/products")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC,
        RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.CRAFT_ENGINEER})
public class ProductController {

    private final ProductService productService;

    /** 构造器注入产品主档 Service。 */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** 创建产品主档并返回数据库生成的主键。 */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createProduct(@Valid @RequestBody ProductSaveReqVO reqVO) {
        return CommonResult.success(productService.createProduct(reqVO));
    }

    /** 修改产品主档；引用、单位和并发版本校验由 Service 执行。 */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateProduct(@PathVariable("id") @Positive Long id,
                                            @Valid @RequestBody ProductUpdateReqVO reqVO) {
        productService.updateProduct(id, reqVO);
        return CommonResult.success(null);
    }

    /** 逻辑删除产品，要求调用方提供当前乐观锁版本。 */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteProduct(@PathVariable("id") @Positive Long id,
                                            @RequestParam("version") @PositiveOrZero Integer version) {
        productService.deleteProduct(id, version);
        return CommonResult.success(null);
    }

    /** 启用或停用产品，Service 负责检查活动业务引用。 */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateProductStatus(@PathVariable("id") @Positive Long id,
                                                  @Valid @RequestBody ProductionStatusReqVO reqVO) {
        productService.updateProductStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /** 查询未删除产品详情并转换为响应 VO。 */
    @GetMapping("/{id}")
    public CommonResult<ProductRespVO> getProduct(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(productService.getProduct(id));
    }

    /** 按产品编码、名称、类型、单位和状态查询分页结果。 */
    @GetMapping("/page")
    public CommonResult<PageResult<ProductRespVO>> getProductPage(@Valid ProductPageReqVO reqVO) {
        return CommonResult.success(productService.getProductPage(reqVO));
    }
}
