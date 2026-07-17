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

/**
 * BOM 版本与明细 Controller。
 *
 * <p>Controller 只负责角色授权、请求参数校验和统一响应包装；版本状态、明细锁定、唯一性及数据库事务均由 Service 处理。
 */
@RestController
@RequestMapping("/api/production/boms")
@RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC,
        RoleCodeConstants.WORKSHOP_MANAGER, RoleCodeConstants.CRAFT_ENGINEER})
public class BomController {

    private final BomService bomService;

    /** 构造器注入 BOM Service，保持 Web 层不直接访问 Repository。 */
    public BomController(BomService bomService) {
        this.bomService = bomService;
    }

    /** 接收已校验的 BOM 创建请求，返回主表生成的数据库主键。 */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createBom(@Valid @RequestBody BomSaveReqVO reqVO) {
        return CommonResult.success(bomService.createBom(reqVO));
    }

    /** 转发 BOM 草稿修改；锁版本和草稿状态由 Service 决定是否允许写入。 */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> updateBom(@PathVariable("id") @Positive Long id,
                                        @Valid @RequestBody BomUpdateReqVO reqVO) {
        bomService.updateBom(id, reqVO);
        return CommonResult.success(null);
    }

    /** 转发逻辑删除请求；客户端锁版本用于阻止旧页面误删最新版本。 */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> deleteBom(
            @PathVariable("id") @Positive Long id,
            @RequestParam("lockVersion") @PositiveOrZero Integer lockVersion) {
        bomService.deleteBom(id, lockVersion);
        return CommonResult.success(null);
    }

    /** 转发生效操作；同产品其他生效版本的停用由 Service 在同一事务内完成。 */
    @PutMapping("/{id}/activate")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> activateBom(@PathVariable("id") @Positive Long id,
                                          @Valid @RequestBody BomActionReqVO reqVO) {
        bomService.activateBom(id, reqVO);
        return CommonResult.success(null);
    }

    /** 转发停用操作，Service 负责校验当前状态和乐观锁版本。 */
    @PutMapping("/{id}/disable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> disableBom(@PathVariable("id") @Positive Long id,
                                         @Valid @RequestBody BomActionReqVO reqVO) {
        bomService.disableBom(id, reqVO);
        return CommonResult.success(null);
    }

    /** 基于来源 BOM 创建新业务版本，返回新版本主键。 */
    @PostMapping("/{id}/new_version")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.PMC, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createNewVersion(@PathVariable("id") @Positive Long id,
                                               @Valid @RequestBody BomNewVersionReqVO reqVO) {
        return CommonResult.success(bomService.createNewVersion(id, reqVO));
    }

    /** 查询 BOM 主表、产品和有效明细组成的聚合详情。 */
    @GetMapping("/{id}")
    public CommonResult<BomRespVO> getBom(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(bomService.getBom(id));
    }

    /** 按编码、产品、版本和状态查询 BOM 分页，空结果由 Service 统一返回空列表。 */
    @GetMapping("/page")
    public CommonResult<PageResult<BomRespVO>> getBomPage(@Valid BomPageReqVO reqVO) {
        return CommonResult.success(bomService.getBomPage(reqVO));
    }
}
