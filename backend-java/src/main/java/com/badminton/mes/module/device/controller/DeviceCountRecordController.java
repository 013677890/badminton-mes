package com.badminton.mes.module.device.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;
import com.badminton.mes.module.device.service.DeviceCountService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
 * 设备计数记录 REST 接口。
 *
 * <p>设备或接入网关调用 {@code /report} 上报计数，后台角色调用详情和分页查询；
 * 请求校验、角色限制和统一响应由 Web 层完成，计数幂等与异常判断由 Service 完成。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
@RestController
@RequestMapping("/api/device/count-records")
public class DeviceCountRecordController {

    private final DeviceCountService countService;

    public DeviceCountRecordController(DeviceCountService countService) {
        this.countService = countService;
    }

    /** 接收一次设备计数上报。 */
    @PostMapping("/report")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceCountReportRespVO> report(
            @Valid @RequestBody DeviceCountReportReqVO request) {
        return CommonResult.success(countService.reportCount(request));
    }

    /** 查询单条计数记录。 */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceCountRecordRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(countService.getCountRecord(id));
    }

    /** 分页查询计数记录。 */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<PageResult<DeviceCountRecordRespVO>> page(
            @Valid DeviceCountRecordPageReqVO request) {
        return CommonResult.success(countService.getCountRecordPage(request));
    }
}
