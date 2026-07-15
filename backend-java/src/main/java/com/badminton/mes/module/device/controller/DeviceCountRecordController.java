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
 * 设备计数报文接入与原始记录查询接口。
 *
 * <p>上报端点接收设备侧不可变计数事实，完成配置校验、幂等识别、增量解释和异常分流；
 * 查询端点用于追溯已落库的原始报文、有效增量及后续任务匹配和报工状态。
 */
@RestController
@RequestMapping("/api/device/count-records")
public class DeviceCountRecordController {

    private final DeviceCountService countService;

    public DeviceCountRecordController(DeviceCountService countService) {
        this.countService = countService;
    }

    /**
     * 接收并处理一条设备计数报文。
     *
     * <p>请求体需通过配置编码、设备编码、采集时间、流水号、非负计数值和报文长度校验。
     * 服务端以配置、采集时间和流水号识别重复上报，并依据累计或增量模式计算有效数量；
     * 设备、工序或计数异常会保留原始记录并转入异常流程，不代表请求报文未落库。
     *
     * @param request 设备侧采集事实及原始报文快照
     * @return 计数记录主键、有效增量、匹配状态、报工状态和异常提示
     */
    @PostMapping("/report")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceCountReportRespVO> report(
            @Valid @RequestBody DeviceCountReportReqVO request) {
        return CommonResult.success(countService.reportCount(request));
    }

    /**
     * 查询单条设备计数原始记录。
     *
     * @param id 计数记录主键，必须为正整数
     * @return 原始计数、解释后增量、设备状态快照及任务处理状态
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceCountRecordRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(countService.getCountRecord(id));
    }

    /**
     * 分页检索设备计数原始记录。
     *
     * <p>可按接入配置、设备、任务匹配状态及设备采集时间闭区间组合过滤；结果按采集业务时间、
     * 主键依次倒序排列。采集时间来自设备报文，不等同于服务端记录入库时间。
     *
     * @param request 分页参数和计数记录筛选条件
     * @return 设备计数记录分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<PageResult<DeviceCountRecordRespVO>> page(
            @Valid DeviceCountRecordPageReqVO request) {
        return CommonResult.success(countService.getCountRecordPage(request));
    }
}
