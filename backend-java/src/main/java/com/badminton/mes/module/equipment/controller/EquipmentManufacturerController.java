package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentManufacturerService;

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

/**
 * 设备制造商主数据 HTTP 接口。
 *
 * <p>负责制造商联系方式及启停状态的创建、修改、逻辑删除、详情和分页请求。控制器仅执行参数绑定、
 * Bean Validation 校验、Service 转发和统一响应包装；制造商编码唯一性、记录存在性以及设备引用检查
 * 由 {@link EquipmentManufacturerService} 处理。
 *
 * <p>请求体字段错误与路径主键约束错误交由全局异常处理器统一转换，端点保持无业务分支的薄层结构。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/equipment/manufacturers")
public class EquipmentManufacturerController {

    /** 设备制造商应用服务，负责唯一性和设备引用完整性校验。 */
    private final EquipmentManufacturerService manufacturerService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param manufacturerService 设备制造商 Service
     */
    public EquipmentManufacturerController(EquipmentManufacturerService manufacturerService) {
        this.manufacturerService = manufacturerService;
    }

    /**
     * 创建设备制造商，编码冲突由 Service 转换为统一业务异常。
     *
     * @param reqVO 已通过字段校验的制造商创建数据，规则见 {@link EquipmentManufacturerSaveReqVO}
     * @return 包含新制造商主键的统一成功响应
     */
    @PostMapping
    public CommonResult<Long> createEquipmentManufacturer(@Valid @RequestBody EquipmentManufacturerSaveReqVO reqVO) {
        return CommonResult.success(manufacturerService.createEquipmentManufacturer(reqVO));
    }

    /**
     * 修改指定制造商的基础资料、联系方式和启停状态。
     *
     * @param id 制造商主键，必须为正数
     * @param reqVO 已通过字段校验的制造商更新数据
     * @return 不携带业务数据的统一成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentManufacturer(@PathVariable("id") @Positive Long id,
                                                           @Valid @RequestBody EquipmentManufacturerSaveReqVO reqVO) {
        manufacturerService.updateEquipmentManufacturer(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除设备制造商。
     *
     * <p>制造商仍被设备台账引用时，Service 将拒绝删除以避免产生失效外键语义。
     *
     * @param id 制造商主键，必须为正数
     * @return 不携带业务数据的统一成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentManufacturer(@PathVariable("id") @Positive Long id) {
        manufacturerService.deleteEquipmentManufacturer(id);
        return CommonResult.success(null);
    }

    /**
     * 按主键查询未逻辑删除的设备制造商详情。
     *
     * @param id 制造商主键，必须为正数
     * @return 设备制造商详情统一响应
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentManufacturerRespVO> getEquipmentManufacturer(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(manufacturerService.getEquipmentManufacturer(id));
    }

    /**
     * 按编码或名称关键字及启停状态分页查询制造商。
     *
     * @param reqVO 由 GET 查询参数绑定形成的分页筛选条件
     * @return 设备制造商分页结果，无匹配数据时列表为空集合
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentManufacturerRespVO>> getEquipmentManufacturerPage(
            @Valid EquipmentManufacturerPageReqVO reqVO) {
        return CommonResult.success(manufacturerService.getEquipmentManufacturerPage(reqVO));
    }
}
