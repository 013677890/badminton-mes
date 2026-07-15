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
 * 设备计数异常查询与处置接口。
 *
 * <p>异常记录由设备计数接入流程自动产生，本接口只提供详情、分页检索和人工终态处置，
 * 不允许直接新增或删除异常。查询面向生产相关角色开放，处置权限收敛到管理及班组负责人。
 */
@RestController
@RequestMapping("/api/device/count-exceptions")
public class DeviceCountExceptionController {

    private final DeviceCountService countService;

    public DeviceCountExceptionController(DeviceCountService countService) {
        this.countService = countService;
    }

    /**
     * 查询单条设备计数异常详情。
     *
     * @param id 异常记录主键，必须为正整数
     * @return 异常来源、类型、原因、处理状态及处置审计信息
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceCountExceptionRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(countService.getCountException(id));
    }

    /**
     * 分页检索设备计数异常。
     *
     * <p>可按接入配置、设备、处理状态及异常创建时间闭区间组合过滤；结果按创建时间、主键依次
     * 倒序排列，便于优先查看最近产生的异常。
     *
     * @param request 分页参数和异常筛选条件
     * @return 设备计数异常分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<PageResult<DeviceCountExceptionRespVO>> page(
            @Valid DeviceCountExceptionPageReqVO request) {
        return CommonResult.success(countService.getCountExceptionPage(request));
    }

    /**
     * 人工结束一条待处理计数异常。
     *
     * <p>路径主键必须为正整数，请求状态只能为已解决或已忽略，处理结果不能为空且最长 500 个字符。
     * 仅待处理异常可以首次处置；成功后记录当前操作人和服务器处理时间，已处置记录不可重复覆盖。
     *
     * @param id 异常记录主键
     * @param request 异常终态与人工处理结论
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}/process")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER})
    public CommonResult<Void> process(@PathVariable @Positive Long id,
                                      @Valid @RequestBody DeviceCountExceptionResolveReqVO request) {
        countService.processCountException(id, request);
        return CommonResult.success(null);
    }
}
