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
 * 设备故障原理 Controller。
 *
 * @author 角色C
 * @date 2026/07/10
 */
@RestController
@RequestMapping("/api/equipment/fault-principles")
public class EquipmentFaultPrincipleController {

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
     * 创建设备故障原理。
     *
     * @param reqVO 创建请求
     * @return 新故障原理主键 id
     */
    @PostMapping
    public CommonResult<Long> createEquipmentFaultPrinciple(
            @Valid @RequestBody EquipmentFaultPrincipleSaveReqVO reqVO) {
        return CommonResult.success(faultPrincipleService.createEquipmentFaultPrinciple(reqVO));
    }

    /**
     * 修改设备故障原理。
     *
     * @param id    故障原理主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentFaultPrinciple(@PathVariable("id") @Positive Long id,
                                                             @Valid @RequestBody EquipmentFaultPrincipleSaveReqVO reqVO) {
        faultPrincipleService.updateEquipmentFaultPrinciple(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除设备故障原理(逻辑删除)。
     *
     * @param id 故障原理主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentFaultPrinciple(@PathVariable("id") @Positive Long id) {
        faultPrincipleService.deleteEquipmentFaultPrinciple(id);
        return CommonResult.success(null);
    }

    /**
     * 查询设备故障原理详情。
     *
     * @param id 故障原理主键
     * @return 设备故障原理详情
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentFaultPrincipleRespVO> getEquipmentFaultPrinciple(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(faultPrincipleService.getEquipmentFaultPrinciple(id));
    }

    /**
     * 分页查询设备故障原理列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentFaultPrincipleRespVO>> getEquipmentFaultPrinciplePage(
            @Valid EquipmentFaultPrinciplePageReqVO reqVO) {
        return CommonResult.success(faultPrincipleService.getEquipmentFaultPrinciplePage(reqVO));
    }
}
