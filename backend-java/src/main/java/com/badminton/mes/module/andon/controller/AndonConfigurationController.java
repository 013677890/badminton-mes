package com.badminton.mes.module.andon.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonConfigurationSaveReqVO;
import com.badminton.mes.module.andon.service.AndonConfigurationService;

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

import static com.badminton.mes.common.security.RoleCodeConstants.ADMIN;
import static com.badminton.mes.common.security.RoleCodeConstants.INSPECTOR;
import static com.badminton.mes.common.security.RoleCodeConstants.OPERATOR;
import static com.badminton.mes.common.security.RoleCodeConstants.TEAM_LEADER;
import static com.badminton.mes.common.security.RoleCodeConstants.WORKSHOP_MANAGER;

/**
 * 安灯异常处理配置接口。
 *
 * <p>配置定义某一安灯类型在全局或指定产线范围内的初始处理人/角色、升级责任主体、响应与升级时限以及通知渠道。
 * 维护权限限于管理员和车间主管，读取权限开放给参与安灯业务的角色。
 */
@RestController
@RequestMapping("/api/andon/configurations")
public class AndonConfigurationController {

    /** 负责配置作用域唯一性、责任主体有效性、时限联动及活动事件保护的配置服务。 */
    private final AndonConfigurationService configurationService;

    /**
     * 注入安灯处理配置服务。
     *
     * @param configurationService 安灯处理配置业务服务
     */
    public AndonConfigurationController(AndonConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    /**
     * 创建安灯处理配置。
     *
     * <p>仅管理员和车间主管可执行。同一安灯类型在同一产线作用域仅允许一条配置；未指定产线时表示全局兜底配置。
     *
     * @param request 类型、作用产线、处理与升级主体、时限和通知渠道
     * @return 新建配置的主键
     */
    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody AndonConfigurationSaveReqVO request) {
        return CommonResult.success(configurationService.createConfiguration(request));
    }

    /**
     * 修改指定安灯处理配置。
     *
     * <p>仅管理员和车间主管可执行；存在该类型的活动事件时服务层禁止修改，防止处理中事件的指派和时限规则漂移。
     *
     * @param id 配置主键
     * @param request 修改后的完整配置字段
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonConfigurationSaveReqVO request) {
        configurationService.updateConfiguration(id, request);
        return CommonResult.success(null);
    }

    /**
     * 删除指定安灯处理配置。
     *
     * <p>仅管理员和车间主管可执行；活动事件仍引用该类型规则时不可删除。
     *
     * @param id 配置主键
     * @return 无业务数据的成功结果
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        configurationService.deleteConfiguration(id);
        return CommonResult.success(null);
    }

    /**
     * 查询单个安灯处理配置。
     *
     * @param id 配置主键
     * @return 包含安灯类型展示信息、责任主体、时限与通知渠道的配置详情
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<AndonConfigurationRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(configurationService.getConfiguration(id));
    }

    /**
     * 分页查询安灯处理配置。
     *
     * <p>参与安灯业务的角色均可按安灯类型、产线、具体处理人和启用状态筛选。
     *
     * @param request 分页参数与配置筛选条件
     * @return 配置分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<PageResult<AndonConfigurationRespVO>> page(
            @Valid AndonConfigurationPageReqVO request) {
        return CommonResult.success(configurationService.getConfigurationPage(request));
    }
}
