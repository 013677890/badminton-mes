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

/** 产品主档 Controller。 */
@RestController
@RequestMapping("/api/production/products")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC,
        RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.CRAFT_ENGINEER})
public class ProductController {

    private final ProductService productService;

    /** @param productService 产品主档服务 */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** @param reqVO 产品创建请求 @return 新产品主键 */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createProduct(@Valid @RequestBody ProductSaveReqVO reqVO) {
        return CommonResult.success(productService.createProduct(reqVO));
    }

    /** @param id 产品主键 @param reqVO 产品修改请求 @return 空数据成功响应 */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateProduct(@PathVariable("id") @Positive Long id,
                                            @Valid @RequestBody ProductUpdateReqVO reqVO) {
        productService.updateProduct(id, reqVO);
        return CommonResult.success(null);
    }

    /** @param id 产品主键 @param version 预期版本 @return 空数据成功响应 */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteProduct(@PathVariable("id") @Positive Long id,
                                            @RequestParam("version") @PositiveOrZero Integer version) {
        productService.deleteProduct(id, version);
        return CommonResult.success(null);
    }

    /** @param id 产品主键 @param reqVO 状态变更请求 @return 空数据成功响应 */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateProductStatus(@PathVariable("id") @Positive Long id,
                                                  @Valid @RequestBody ProductionStatusReqVO reqVO) {
        productService.updateProductStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /** @param id 产品主键 @return 产品详情 */
    @GetMapping("/{id}")
    public CommonResult<ProductRespVO> getProduct(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(productService.getProduct(id));
    }

    /** @param reqVO 分页筛选条件 @return 产品分页结果 */
    @GetMapping("/page")
    public CommonResult<PageResult<ProductRespVO>> getProductPage(@Valid ProductPageReqVO reqVO) {
        return CommonResult.success(productService.getProductPage(reqVO));
    }
}
