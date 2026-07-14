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
 * 设备制造商 Controller。
 *
 * <p>Web 层职责保持单薄：声明式参数校验、转发 Service、包装统一响应，
 * 不写业务规则。路径全小写、单词用下划线分隔、资源名词用复数。
 *
 * <p>校验失败由 {@code GlobalExceptionHandler} 统一转为 A0400 响应；
 * 请求体字段校验触发 MethodArgumentNotValidException，路径参数约束触发
 * HandlerMethodValidationException。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/equipment/manufacturers")
public class EquipmentManufacturerController {

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
     * 创建设备制造商。
     *
     * @param reqVO 创建请求，字段规则见 {@link EquipmentManufacturerSaveReqVO}
     * @return 新制造商主键 id
     */
    @PostMapping
    public CommonResult<Long> createEquipmentManufacturer(@Valid @RequestBody EquipmentManufacturerSaveReqVO reqVO) {
        return CommonResult.success(manufacturerService.createEquipmentManufacturer(reqVO));
    }

    /**
     * 修改设备制造商。
     *
     * @param id    制造商主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentManufacturer(@PathVariable("id") @Positive Long id,
                                                           @Valid @RequestBody EquipmentManufacturerSaveReqVO reqVO) {
        manufacturerService.updateEquipmentManufacturer(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除设备制造商(逻辑删除)。
     *
     * <p>该制造商下存在设备时不允许删除。
     *
     * @param id 制造商主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentManufacturer(@PathVariable("id") @Positive Long id) {
        manufacturerService.deleteEquipmentManufacturer(id);
        return CommonResult.success(null);
    }

    /**
     * 查询设备制造商详情。
     *
     * @param id 制造商主键
     * @return 制造商详情
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentManufacturerRespVO> getEquipmentManufacturer(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(manufacturerService.getEquipmentManufacturer(id));
    }

    /**
     * 分页查询设备制造商列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果，无数据时 list 为空集合
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentManufacturerRespVO>> getEquipmentManufacturerPage(
            @Valid EquipmentManufacturerPageReqVO reqVO) {
        return CommonResult.success(manufacturerService.getEquipmentManufacturerPage(reqVO));
    }
}
