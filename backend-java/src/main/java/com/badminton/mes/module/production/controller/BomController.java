package com.badminton.mes.module.production.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.production.controller.vo.BomActionReqVO;
import com.badminton.mes.module.production.controller.vo.BomNewVersionReqVO;
import com.badminton.mes.module.production.controller.vo.BomPageReqVO;
import com.badminton.mes.module.production.controller.vo.BomRespVO;
import com.badminton.mes.module.production.controller.vo.BomSaveReqVO;
import com.badminton.mes.module.production.controller.vo.BomUpdateReqVO;
import com.badminton.mes.module.production.service.BomService;

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

/** BOM 版本与明细 Controller。 */
@RestController
@RequestMapping("/api/production/boms")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC,
        RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.CRAFT_ENGINEER})
public class BomController {

    private final BomService bomService;

    /** @param bomService BOM 版本服务 */
    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    /** @param reqVO BOM 创建请求 @return 新 BOM 主键 */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createBom(@Valid @RequestBody BomSaveReqVO reqVO) {
        return CommonResult.success(bomService.createBom(reqVO));
    }

    /** @param id BOM 主键 @param reqVO BOM 修改请求 @return 空数据成功响应 */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> updateBom(@PathVariable("id") @Positive Long id,
                                        @Valid @RequestBody BomUpdateReqVO reqVO) {
        bomService.updateBom(id, reqVO);
        return CommonResult.success(null);
    }

    /** @param id BOM 主键 @param lockVersion 预期锁版本 @return 空数据成功响应 */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> deleteBom(
            @PathVariable("id") @Positive Long id,
            @RequestParam("lockVersion") @PositiveOrZero Integer lockVersion) {
        bomService.deleteBom(id, lockVersion);
        return CommonResult.success(null);
    }

    /** @param id BOM 主键 @param reqVO 生效请求 @return 空数据成功响应 */
    @PutMapping("/{id}/activate")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> activateBom(@PathVariable("id") @Positive Long id,
                                          @Valid @RequestBody BomActionReqVO reqVO) {
        bomService.activateBom(id, reqVO);
        return CommonResult.success(null);
    }

    /** @param id BOM 主键 @param reqVO 停用请求 @return 空数据成功响应 */
    @PutMapping("/{id}/disable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> disableBom(@PathVariable("id") @Positive Long id,
                                         @Valid @RequestBody BomActionReqVO reqVO) {
        bomService.disableBom(id, reqVO);
        return CommonResult.success(null);
    }

    /** @param id 来源 BOM 主键 @param reqVO 新版本请求 @return 新 BOM 主键 */
    @PostMapping("/{id}/new_version")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createNewVersion(@PathVariable("id") @Positive Long id,
                                               @Valid @RequestBody BomNewVersionReqVO reqVO) {
        return CommonResult.success(bomService.createNewVersion(id, reqVO));
    }

    /** @param id BOM 主键 @return BOM 聚合详情 */
    @GetMapping("/{id}")
    public CommonResult<BomRespVO> getBom(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(bomService.getBom(id));
    }

    /** @param reqVO 分页筛选条件 @return BOM 分页结果 */
    @GetMapping("/page")
    public CommonResult<PageResult<BomRespVO>> getBomPage(@Valid BomPageReqVO reqVO) {
        return CommonResult.success(bomService.getBomPage(reqVO));
    }
}
