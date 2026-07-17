package com.badminton.mes.module.system.controller;

import java.util.List;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.module.system.controller.vo.RoleRespVO;
import com.badminton.mes.module.system.controller.vo.RoleUserRespVO;
import com.badminton.mes.module.system.service.RoleService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Positive;

/**
 * 系统角色 Controller。角色为种子数据固化，只提供查询。
 *
 * @author 张竹灏
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    private final RoleService roleService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param roleService 角色 Service
     */
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 查询启用角色列表，用户管理表单下拉使用。
     *
     * @return 启用角色列表，无数据时为空集合(API-002)
     */
    @GetMapping
    public CommonResult<List<RoleRespVO>> getEnabledRoles() {
        return CommonResult.success(roleService.getEnabledRoles());
    }

    /**
     * 按角色反查启用用户(安灯按角色匹配处理人等场景)，登录即可访问。
     *
     * @param id 角色主键
     * @return 拥有该角色的启用用户列表，无数据时为空集合(API-002)
     */
    @GetMapping("/{id}/users")
    public CommonResult<List<RoleUserRespVO>> getRoleUsers(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(roleService.getRoleUsers(id));
    }
}
