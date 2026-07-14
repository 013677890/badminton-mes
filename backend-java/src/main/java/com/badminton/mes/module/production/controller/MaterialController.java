package com.badminton.mes.module.production.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.vo.MaterialPageReqVO;
import com.badminton.mes.module.production.controller.vo.MaterialRespVO;
import com.badminton.mes.module.production.controller.vo.MaterialSaveReqVO;
import com.badminton.mes.module.production.controller.vo.MaterialUpdateReqVO;
import com.badminton.mes.module.production.controller.vo.ProductionStatusReqVO;
import com.badminton.mes.module.production.service.MaterialService;

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

/** 物料主档 Controller。 */
@RestController
@RequestMapping("/api/production/materials")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC,
        RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.CRAFT_ENGINEER})
public class MaterialController {

    private final MaterialService materialService;

    /** @param materialService 物料主档服务 */
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /** @param reqVO 物料创建请求 @return 新物料主键 */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createMaterial(@Valid @RequestBody MaterialSaveReqVO reqVO) {
        return CommonResult.success(materialService.createMaterial(reqVO));
    }

    /** @param id 物料主键 @param reqVO 物料修改请求 @return 空数据成功响应 */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateMaterial(@PathVariable("id") @Positive Long id,
                                             @Valid @RequestBody MaterialUpdateReqVO reqVO) {
        materialService.updateMaterial(id, reqVO);
        return CommonResult.success(null);
    }

    /** @param id 物料主键 @param version 预期版本 @return 空数据成功响应 */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteMaterial(@PathVariable("id") @Positive Long id,
                                             @RequestParam("version") @PositiveOrZero Integer version) {
        materialService.deleteMaterial(id, version);
        return CommonResult.success(null);
    }

    /** @param id 物料主键 @param reqVO 状态变更请求 @return 空数据成功响应 */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateMaterialStatus(@PathVariable("id") @Positive Long id,
                                                   @Valid @RequestBody ProductionStatusReqVO reqVO) {
        materialService.updateMaterialStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /** @param id 物料主键 @return 物料详情 */
    @GetMapping("/{id}")
    public CommonResult<MaterialRespVO> getMaterial(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(materialService.getMaterial(id));
    }

    /** @param reqVO 分页筛选条件 @return 物料分页结果 */
    @GetMapping("/page")
    public CommonResult<PageResult<MaterialRespVO>> getMaterialPage(@Valid MaterialPageReqVO reqVO) {
        return CommonResult.success(materialService.getMaterialPage(reqVO));
    }
}
