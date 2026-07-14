package com.badminton.mes.module.device.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;
import com.badminton.mes.module.device.service.DeviceAccessConfigService;

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
import static com.badminton.mes.common.security.RoleCodeConstants.OPERATOR;
import static com.badminton.mes.common.security.RoleCodeConstants.PMC;
import static com.badminton.mes.common.security.RoleCodeConstants.TEAM_LEADER;
import static com.badminton.mes.common.security.RoleCodeConstants.WORKSHOP_MANAGER;

/** 设备接入配置接口。 */
@RestController
@RequestMapping("/api/device/access-configs")
public class DeviceAccessConfigController {

    private final DeviceAccessConfigService accessConfigService;

    public DeviceAccessConfigController(DeviceAccessConfigService accessConfigService) {
        this.accessConfigService = accessConfigService;
    }

    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody DeviceAccessConfigSaveReqVO request) {
        return CommonResult.success(accessConfigService.createAccessConfig(request));
    }

    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody DeviceAccessConfigSaveReqVO request) {
        accessConfigService.updateAccessConfig(id, request);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        accessConfigService.deleteAccessConfig(id);
        return CommonResult.success(null);
    }

    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceAccessConfigRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(accessConfigService.getAccessConfig(id));
    }

    @GetMapping("/page")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<PageResult<DeviceAccessConfigRespVO>> page(
            @Valid DeviceAccessConfigPageReqVO request) {
        return CommonResult.success(accessConfigService.getAccessConfigPage(request));
    }
}
