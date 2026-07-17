package com.badminton.mes.module.equipment.controller;

import com.badminton.mes.common.core.CommonResult;
import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerPageReqVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;
import com.badminton.mes.module.equipment.service.EquipmentLedgerService;

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
 * 设备台账主数据 HTTP 接口。
 *
 * <p>设备台账统一维护设备身份、归属位置、制造信息、运行状态和启停状态，是保养与报修业务引用设备
 * 的入口。本控制器仅处理参数校验、调用 {@link EquipmentLedgerService} 和统一响应包装；设备编码
 * 唯一性、类别与制造商有效性、车间和产线归属关系以及删除引用约束由 Service 处理。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/equipment/ledgers")
public class EquipmentLedgerController {

    /** 设备台账应用服务，负责主数据及跨模块引用的一致性。 */
    private final EquipmentLedgerService ledgerService;

    /**
     * 构造器注入，依赖不可变。
     *
     * @param ledgerService 设备台账 Service
     */
    public EquipmentLedgerController(EquipmentLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * 创建设备台账并校验其类别、制造商及组织归属信息。
     *
     * @param reqVO 已通过字段校验的设备台账创建数据
     * @return 包含新设备主键的统一成功响应
     */
    @PostMapping
    public CommonResult<Long> createEquipmentLedger(@Valid @RequestBody EquipmentLedgerSaveReqVO reqVO) {
        return CommonResult.success(ledgerService.createEquipmentLedger(reqVO));
    }

    /**
     * 修改指定设备的基础资料、组织归属、业务状态和启停状态。
     *
     * @param id 设备台账主键，必须为正数
     * @param reqVO 已通过字段校验的设备台账更新数据
     * @return 不携带业务数据的统一成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentLedger(@PathVariable("id") @Positive Long id,
                                                     @Valid @RequestBody EquipmentLedgerSaveReqVO reqVO) {
        ledgerService.updateEquipmentLedger(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 逻辑删除设备台账，保养、报修等业务引用存在时由 Service 拒绝删除。
     *
     * @param id 设备台账主键，必须为正数
     * @return 不携带业务数据的统一成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentLedger(@PathVariable("id") @Positive Long id) {
        ledgerService.deleteEquipmentLedger(id);
        return CommonResult.success(null);
    }

    /**
     * 按主键查询未逻辑删除的设备台账详情。
     *
     * @param id 设备台账主键，必须为正数
     * @return 设备台账详情统一响应
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentLedgerRespVO> getEquipmentLedger(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(ledgerService.getEquipmentLedger(id));
    }

    /**
     * 按关键字、类别、制造商、设备状态、组织归属和启停状态分页查询设备。
     *
     * @param reqVO 由 GET 查询参数绑定形成的分页筛选条件
     * @return 设备台账分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentLedgerRespVO>> getEquipmentLedgerPage(
            @Valid EquipmentLedgerPageReqVO reqVO) {
        return CommonResult.success(ledgerService.getEquipmentLedgerPage(reqVO));
    }
}
