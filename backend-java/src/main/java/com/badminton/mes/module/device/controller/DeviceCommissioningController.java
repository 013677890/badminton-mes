package com.badminton.mes.module.device.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.common.security.RequiresRoles;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;
import com.badminton.mes.module.device.service.DeviceCommissioningService;

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
 * 设备接入联调记录接口。
 *
 * <p>用于登记和查询采集配置的通信链路、报文格式及综合联调结论。每次登记都会形成不可变的
 * 联调历史，并把最新综合结论同步到接入配置；失败结论会使正式采集回到停用状态。
 */
@RestController
@RequestMapping("/api/device/commissioning-records")
public class DeviceCommissioningController {

    private final DeviceCommissioningService commissioningService;

    public DeviceCommissioningController(DeviceCommissioningService commissioningService) {
        this.commissioningService = commissioningService;
    }

    /**
     * 登记一次设备接入联调结果。
     *
     * <p>请求体校验关联配置、联调时间、分项结果、综合结论和文本长度；联调时间不得晚于服务器
     * 当前时间，综合通过要求通信及数据格式均成功，任一检查失败时必须填写问题说明。
     *
     * @param request 本次联调的业务时间、检查结果和留存报文
     * @return 新增联调记录的主键
     */
    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody DeviceCommissioningSaveReqVO request) {
        return CommonResult.success(commissioningService.createCommissioningRecord(request));
    }

    /**
     * 查询单条设备联调记录详情。
     *
     * @param id 联调记录主键，必须为正整数
     * @return 分项检查、综合结论、测试人员、业务时间及问题复现信息
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceCommissioningRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(commissioningService.getCommissioningRecord(id));
    }

    /**
     * 分页检索设备联调历史。
     *
     * <p>可按接入配置、综合结论及实际联调时间闭区间组合过滤；结果按联调业务时间、主键依次
     * 倒序排列，因此展示的是历史事实，而不是接入配置当前状态的替代值。
     *
     * @param request 分页参数和联调记录筛选条件
     * @return 设备联调记录分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<PageResult<DeviceCommissioningRespVO>> page(
            @Valid DeviceCommissioningPageReqVO request) {
        return CommonResult.success(commissioningService.getCommissioningRecordPage(request));
    }
}
