package com.badminton.mes.module.craft.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.common.security.RoleCodeConstants;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogPageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteChangeLogRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteNewVersionReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRoutePageReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteRespVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteSaveReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteStatusReqVO;
import com.badminton.mes.module.craft.controller.vo.CraftRouteUpdateReqVO;
import com.badminton.mes.module.craft.service.CraftRouteService;

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
 * 工艺路线 Controller。
 *
 * <p>写操作限管理员和工艺工程师，查询接口登录即可访问。
 *
 * @author 张竹灏
 * @date 2026/07/10
 */
@Validated
@RestController
@RequestMapping("/api/craft/routes")
public class CraftRouteController {

    private final CraftRouteService routeService;

    /**
     * 构造器注入。
     *
     * @param routeService 工艺路线 Service
     */
    public CraftRouteController(CraftRouteService routeService) {
        this.routeService = routeService;
    }

    /**
     * 创建草稿工艺路线聚合。
     *
     * @param reqVO 创建请求
     * @return 新路线主键
     */
    @PostMapping
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createRoute(@Valid @RequestBody CraftRouteSaveReqVO reqVO) {
        return CommonResult.success(routeService.createRoute(reqVO));
    }

    /**
     * 按预期版本修改草稿路线聚合。
     *
     * @param id    路线主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> updateRoute(@PathVariable("id") @Positive Long id,
                                          @Valid @RequestBody CraftRouteUpdateReqVO reqVO) {
        routeService.updateRoute(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 按预期版本审核草稿路线生效。
     *
     * @param id    路线主键
     * @param reqVO 审核请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/approve")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> approveRoute(@PathVariable("id") @Positive Long id,
                                           @Valid @RequestBody CraftRouteStatusReqVO reqVO) {
        routeService.approveRoute(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 按预期版本停用生效路线。
     *
     * @param id    路线主键
     * @param reqVO 停用请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}/disable")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> disableRoute(@PathVariable("id") @Positive Long id,
                                           @Valid @RequestBody CraftRouteStatusReqVO reqVO) {
        routeService.disableRoute(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 基于生效路线创建新业务版本草稿。
     *
     * @param id    源路线主键
     * @param reqVO 新版本请求
     * @return 新版本路线主键
     */
    @PostMapping("/{id}/versions")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Long> createRouteVersion(@PathVariable("id") @Positive Long id,
                                                 @Valid @RequestBody CraftRouteNewVersionReqVO reqVO) {
        return CommonResult.success(routeService.createRouteVersion(id, reqVO));
    }

    /**
     * 按预期版本逻辑删除草稿路线聚合。
     *
     * @param id      路线主键
     * @param version 客户端读取时的版本号
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({RoleCodeConstants.ADMIN, RoleCodeConstants.CRAFT_ENGINEER})
    public CommonResult<Void> deleteRoute(
            @PathVariable("id") @Positive Long id,
            @RequestParam("version") @PositiveOrZero Integer version) {
        routeService.deleteRoute(id, version);
        return CommonResult.success(null);
    }

    /**
     * 查询产品默认生效路线聚合详情。
     *
     * @param productId 产品主键
     * @return 默认路线详情
     */
    @GetMapping("/default")
    public CommonResult<CraftRouteRespVO> getDefaultRoute(
            @RequestParam("productId") @Positive Long productId) {
        return CommonResult.success(routeService.getDefaultRoute(productId));
    }

    /**
     * 分页查询路线主档。
     *
     * @param reqVO 分页查询请求
     * @return 路线分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<CraftRouteRespVO>> getRoutePage(
            @Valid CraftRoutePageReqVO reqVO) {
        return CommonResult.success(routeService.getRoutePage(reqVO));
    }

    /**
     * 查询路线聚合详情。
     *
     * @param id 路线主键
     * @return 路线详情
     */
    @GetMapping("/{id}")
    public CommonResult<CraftRouteRespVO> getRoute(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(routeService.getRoute(id));
    }

    /**
     * 分页查询路线变更日志。
     *
     * @param id    路线主键
     * @param reqVO 分页请求
     * @return 变更日志分页结果
     */
    @GetMapping("/{id}/change_logs")
    public CommonResult<PageResult<CraftRouteChangeLogRespVO>> getRouteChangeLogPage(
            @PathVariable("id") @Positive Long id,
            @Valid CraftRouteChangeLogPageReqVO reqVO) {
        return CommonResult.success(routeService.getRouteChangeLogPage(id, reqVO));
    }
}
