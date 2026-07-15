package com.badminton.mes.module.andon.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.andon.controller.vo.AndonTypePageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonTypeSaveReqVO;
import com.badminton.mes.module.andon.service.AndonTypeService;

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
 * 安灯类型主数据接口。
 *
 * <p>类型定义异常分类、处理模式、默认责任角色、响应时限、通知渠道和灯控开关，是事件创建时选择状态流转、
 * 指派与现场灯控策略的基础。维护权限限于管理员和车间主管，其他安灯角色仅可读取。
 */
@RestController
@RequestMapping("/api/andon/types")
public class AndonTypeController {

    /** 负责类型编码唯一性、处理模式规则、启停状态及关联数据保护的类型服务。 */
    private final AndonTypeService typeService;

    /**
     * 注入安灯类型服务。
     *
     * @param typeService 安灯类型业务服务
     */
    public AndonTypeController(AndonTypeService typeService) {
        this.typeService = typeService;
    }

    /**
     * 创建安灯类型。
     *
     * <p>仅管理员和车间主管可执行；协助处理模式必须同时配置响应时限、责任角色和通知渠道，
     * 灯控开关决定该类型事件发起时是否尝试开启现场设备安灯。
     *
     * @param request 类型编码、分类、处理模式及默认处理规则
     * @return 新建类型的主键
     */
    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody AndonTypeSaveReqVO request) {
        return CommonResult.success(typeService.createType(request));
    }

    /**
     * 修改指定安灯类型。
     *
     * <p>仅管理员和车间主管可执行，服务层会校验编码唯一性和处理模式所需的配套字段。
     *
     * @param id 类型主键
     * @param request 修改后的类型业务字段
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody AndonTypeSaveReqVO request) {
        typeService.updateType(id, request);
        return CommonResult.success(null);
    }

    /**
     * 删除指定安灯类型。
     *
     * <p>仅管理员和车间主管可执行；类型已被原因、处理配置或事件引用时禁止删除，以保留历史业务关联。
     *
     * @param id 类型主键
     * @return 无业务数据的成功结果
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        typeService.deleteType(id);
        return CommonResult.success(null);
    }

    /**
     * 查询单个安灯类型。
     *
     * @param id 类型主键
     * @return 类型规则、灯控开关、启用状态和审计时间
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<AndonTypeRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(typeService.getType(id));
    }

    /**
     * 分页查询安灯类型。
     *
     * <p>参与安灯业务的角色均可按关键字、异常类别、处理方式和启用状态筛选类型主数据。
     *
     * @param request 分页参数与类型筛选条件
     * @return 类型分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<PageResult<AndonTypeRespVO>> page(@Valid AndonTypePageReqVO request) {
        return CommonResult.success(typeService.getTypePage(request));
    }
}
