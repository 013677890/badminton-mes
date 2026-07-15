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

/**
 * 设备接入配置管理接口。
 *
 * <p>负责维护设备采集点与台账设备、工序及产线的绑定关系，并向生产管理角色提供联调状态、
 * 正式采集状态和最近通信时间查询。新增、修改、删除属于配置治理操作，仅管理员和车间主任可执行。
 */
@RestController
@RequestMapping("/api/device/access-configs")
public class DeviceAccessConfigController {

    private final DeviceAccessConfigService accessConfigService;

    public DeviceAccessConfigController(DeviceAccessConfigService accessConfigService) {
        this.accessConfigService = accessConfigService;
    }

    /**
     * 新建设备接入配置。
     *
     * <p>请求体需通过编码、名称、关联主键、枚举值及长度边界校验；服务端还会校验配置编码和
     * “设备 + 采集点编码”的唯一性。新配置固定从未联调、停用状态开始，不能借由请求直接启用。
     *
     * @param request 接入配置业务字段
     * @return 新增配置的主键
     */
    @PostMapping
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Long> create(@Valid @RequestBody DeviceAccessConfigSaveReqVO request) {
        return CommonResult.success(accessConfigService.createAccessConfig(request));
    }

    /**
     * 修改指定接入配置的可维护字段。
     *
     * <p>路径主键必须为正整数，请求体执行完整校验；配置编码及采集点组合仍需满足唯一性。
     * 当请求启用正式采集时，目标配置的最近联调状态必须已经通过。
     *
     * @param id 接入配置主键
     * @param request 更新后的接入配置业务字段
     * @return 无业务数据的成功结果
     */
    @PutMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> update(@PathVariable @Positive Long id,
                                     @Valid @RequestBody DeviceAccessConfigSaveReqVO request) {
        accessConfigService.updateAccessConfig(id, request);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除指定接入配置。
     *
     * <p>路径主键必须为正整数。仅没有联调记录和计数记录的配置可以删除；删除后配置会被停用，
     * 并从详情及分页查询中排除，以保留主键和审计边界。
     *
     * @param id 接入配置主键
     * @return 无业务数据的成功结果
     */
    @DeleteMapping("/{id}")
    @RequiresRoles({ADMIN, WORKSHOP_MANAGER})
    public CommonResult<Void> delete(@PathVariable @Positive Long id) {
        accessConfigService.deleteAccessConfig(id);
        return CommonResult.success(null);
    }

    /**
     * 查询单个未删除接入配置的完整详情。
     *
     * @param id 接入配置主键，必须为正整数
     * @return 配置绑定关系、计数规则、联调状态、启用状态及通信时间
     */
    @GetMapping("/{id}")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<DeviceAccessConfigRespVO> get(@PathVariable @Positive Long id) {
        return CommonResult.success(accessConfigService.getAccessConfig(id));
    }

    /**
     * 分页查询未删除的设备接入配置。
     *
     * <p>支持关键字、设备、工序、联调状态和启用状态组合过滤，分页参数及各筛选字段由请求对象
     * 统一校验；结果按主键倒序返回，超出末页时收敛到实际最后一页。
     *
     * @param request 分页参数与组合筛选条件
     * @return 接入配置分页结果
     */
    @GetMapping("/page")
    @RequiresRoles({ADMIN, PMC, WORKSHOP_MANAGER, TEAM_LEADER, OPERATOR})
    public CommonResult<PageResult<DeviceAccessConfigRespVO>> page(
            @Valid DeviceAccessConfigPageReqVO request) {
        return CommonResult.success(accessConfigService.getAccessConfigPage(request));
    }
}
