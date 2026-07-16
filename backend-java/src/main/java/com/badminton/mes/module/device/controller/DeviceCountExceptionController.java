package com.badminton.mes.module.device.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionResolveReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.device.service.DeviceCountService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import static com.badminton.mes.common.security.RoleCodeConstants.ADMIN;
import static com.badminton.mes.common.security.RoleCodeConstants.OPERATOR;
import static com.badminton.mes.common.security.RoleCodeConstants.PMC;
import static com.badminton.mes.common.security.RoleCodeConstants.TEAM_LEADER;
import static com.badminton.mes.common.security.RoleCodeConstants.WORKSHOP_MANAGER;

/**
 * 设备计数异常 REST 接口。
 *
 * <p>异常由计数 Service 在上报时自动生成，本 Controller 提供查询和人工处理入口；
 * 处理接口仅向管理角色开放，避免普通操作员直接修改异常结论。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@RestController
@RequestMapping("/api/device/count-exceptions")
public class DeviceCountExceptionController {

    private final DeviceCountService countService;

    public DeviceCountExceptionController(DeviceCountService countService) {
        this.countService = countService;
    }

    /** 查询单条计数异常。 */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceCountExceptionRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(countService.getCountException(id));
    }

    /** 分页查询计数异常。 */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<PageResult<DeviceCountExceptionRespVO>> page(
            @Valid DeviceCountExceptionPageReqVO request) {
        return CommonResult.success(countService.getCountExceptionPage(request));
    }

    /** 处理计数异常并记录处理意见。 */
    @PutMapping("/{id}/process")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<Void> process(@PathVariable @Positive Long id,
                                      @Valid @RequestBody DeviceCountExceptionResolveReqVO request) {
        countService.processCountException(id, request);
        return CommonResult.success(null);
    }
}
