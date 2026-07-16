package com.badminton.mes.module.andon.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.andon.controller.vo.AndonReasonPageReqVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonRespVO;
import com.badminton.mes.module.andon.controller.vo.AndonReasonSaveReqVO;
import com.badminton.mes.module.andon.service.AndonReasonService;

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
 * 安灯异常原因字典接口。
 *
 * <p>原因按安灯类型归类，供事件发起时选择预判原因、处理时登记实际原因。维护操作仅允许管理员和车间主管执行，
 * 其他安灯业务角色仅拥有详情与分页读取权限，避免现场操作人员随意改变原因口径。
 */
@RestController
@RequestMapping("/api/andon/reasons")
public class AndonReasonController {

    /** 负责原因编码唯一性、类型归属、启停状态和引用约束的原因服务。 */
    private final AndonReasonService reasonService;

    /**
     * 注入安灯原因服务。
     *
     * @param reasonService 安灯原因业务服务
     */
    public AndonReasonController(AndonReasonService reasonService) {
        this.reasonService = reasonService;
    }

    /**
     * 创建安灯异常原因。
     *
     * <p>仅管理员和车间主管可维护；原因编码用于稳定标识，类型主键决定该原因可用于哪类安灯事件。
     *
     * @param request 原因编码、名称、所属类型、描述及启用状态
     * @return 新建原因的主键
     */
    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody AndonReasonSaveReqVO request) {
        return CommonResult.success(reasonService.createReason(request));
    }

    /**
     * 修改指定安灯异常原因。
     *
     * <p>仅管理员和车间主管可执行，服务层负责校验原因存在、编码冲突及类型有效性。
     *
     * @param id 原因主键
     * @param request 修改后的原因业务字段
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody AndonReasonSaveReqVO request) {
        reasonService.updateReason(id, request);
        return CommonResult.success(null);
    }

    /**
     * 删除指定安灯异常原因。
     *
     * <p>仅管理员和车间主管可执行；删除受事件引用关系约束，避免历史事件的原因语义丢失。
     *
     * @param id 原因主键
     * @return 无业务数据的成功结果
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        reasonService.deleteReason(id);
        return CommonResult.success(null);
    }

    /**
     * 查询单个安灯异常原因。
     *
     * @param id 原因主键
     * @return 包含所属类型展示信息和审计时间的原因详情
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<AndonReasonRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(reasonService.getReason(id));
    }

    /**
     * 分页查询安灯异常原因。
     *
     * <p>参与安灯业务的角色均可按关键字、所属类型和启用状态筛选原因字典。
     *
     * @param request 分页参数与原因筛选条件
     * @return 原因分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR, INSPECTOR})
    public CommonResult<PageResult<AndonReasonRespVO>> page(@Valid AndonReasonPageReqVO request) {
        return CommonResult.success(reasonService.getReasonPage(request));
    }
}
