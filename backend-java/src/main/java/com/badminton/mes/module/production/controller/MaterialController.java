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

/**
 * 物料主档 Controller。
 *
 * <p>Controller 仅负责 HTTP 参数校验、权限声明和响应包装；物料引用、单位锁和逻辑删除规则由 Service 统一处理。
 */
@RestController
@RequestMapping("/api/production/materials")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC,
        RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.CRAFT_ENGINEER})
public class MaterialController {

    private final MaterialService materialService;

    /** 构造器注入物料主档 Service。 */
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    /** 创建物料主档并返回数据库生成的主键。 */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Long> createMaterial(@Valid @RequestBody MaterialSaveReqVO reqVO) {
        return CommonResult.success(materialService.createMaterial(reqVO));
    }

    /** 修改物料主档；单位不可变和引用校验由 Service 执行。 */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateMaterial(@PathVariable("id") @Positive Long id,
                                             @Valid @RequestBody MaterialUpdateReqVO reqVO) {
        materialService.updateMaterial(id, reqVO);
        return CommonResult.success(null);
    }

    /** 逻辑删除物料，使用乐观锁版本避免旧页面删除新数据。 */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> deleteMaterial(@PathVariable("id") @Positive Long id,
                                             @RequestParam("version") @PositiveOrZero Integer version) {
        materialService.deleteMaterial(id, version);
        return CommonResult.success(null);
    }

    /** 启用或停用物料，停用前由 Service 检查生效 BOM 和活动工单。 */
    @PutMapping("/{id}/status")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC})
    public CommonResult<Void> updateMaterialStatus(@PathVariable("id") @Positive Long id,
                                                   @Valid @RequestBody ProductionStatusReqVO reqVO) {
        materialService.updateMaterialStatus(id, reqVO);
        return CommonResult.success(null);
    }

    /** 查询未删除物料详情并转换为响应 VO。 */
    @GetMapping("/{id}")
    public CommonResult<MaterialRespVO> getMaterial(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(materialService.getMaterial(id));
    }

    /** 按物料编码、名称、类型、单位、关键物料标识和状态查询分页结果。 */
    @GetMapping("/page")
    public CommonResult<PageResult<MaterialRespVO>> getMaterialPage(@Valid MaterialPageReqVO reqVO) {
        return CommonResult.success(materialService.getMaterialPage(reqVO));
    }
}
