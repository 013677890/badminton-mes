package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrinciplePageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentFaultPrincipleSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentFaultPrincipleService;

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
 * 设备故障原理主数据 HTTP 接口。
 *
 * <p>故障原理沉淀可复用的故障分类、等级和建议处理方案，可限定适用设备类别。本控制器负责参数校验、
 * 调用 {@link EquipmentFaultPrincipleService} 和统一响应包装；编码唯一性、适用类别存在性以及删除时
 * 的报修单引用检查由 Service 统一处理。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@RestController
@RequestMapping("/api/equipment/fault-principles")
public class EquipmentFaultPrincipleController {

    /** 设备故障原理应用服务，负责主数据约束及报修引用完整性。 */
    private final EquipmentFaultPrincipleService faultPrincipleService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param faultPrincipleService 设备故障原理 Service
     */
    public EquipmentFaultPrincipleController(EquipmentFaultPrincipleService faultPrincipleService) {
        this.faultPrincipleService = faultPrincipleService;
    }

    /**
     * 创建设备故障原理，可通过类别主键限定其适用范围。
     *
     * @param reqVO 已通过字段校验的故障原理创建数据
     * @return 包含新故障原理主键的统一成功响应
     */
    @PostMapping
    public CommonResult<Long> createEquipmentFaultPrinciple(
            @Valid @RequestBody EquipmentFaultPrincipleSaveReqVO reqVO) {
        return CommonResult.success(faultPrincipleService.createEquipmentFaultPrinciple(reqVO));
    }

    /**
     * 修改指定故障原理的定义、建议方案、排序及启停状态。
     *
     * @param id 故障原理主键，必须为正数
     * @param reqVO 已通过字段校验的故障原理更新数据
     * @return 不携带业务数据的统一成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentFaultPrinciple(@PathVariable("id") @Positive Long id,
                                                             @Valid @RequestBody EquipmentFaultPrincipleSaveReqVO reqVO) {
        faultPrincipleService.updateEquipmentFaultPrinciple(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除指定故障原理，关联报修任务等引用约束由 Service 校验。
     *
     * @param id 故障原理主键，必须为正数
     * @return 不携带业务数据的统一成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentFaultPrinciple(@PathVariable("id") @Positive Long id) {
        faultPrincipleService.deleteEquipmentFaultPrinciple(id);
        return CommonResult.success(null);
    }

    /**
     * 按主键查询未逻辑删除的设备故障原理详情。
     *
     * @param id 故障原理主键，必须为正数
     * @return 设备故障原理详情统一响应
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentFaultPrincipleRespVO> getEquipmentFaultPrinciple(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(faultPrincipleService.getEquipmentFaultPrinciple(id));
    }

    /**
     * 按关键字、适用设备类别、故障等级和启停状态分页查询故障原理。
     *
     * @param reqVO 由 GET 查询参数绑定形成的分页筛选条件
     * @return 设备故障原理分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentFaultPrincipleRespVO>> getEquipmentFaultPrinciplePage(
            @Valid EquipmentFaultPrinciplePageReqVO reqVO) {
        return CommonResult.success(faultPrincipleService.getEquipmentFaultPrinciplePage(reqVO));
    }
}
