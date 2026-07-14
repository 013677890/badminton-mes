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
 * 设备台账 Controller。
 *
 * <p>设备台账是设备管理的主数据入口，路径使用复数资源名 ledgers。
 *
 * @author 角色C
 * @date 2026/07/09
 */
@RestController
@RequestMapping("/api/equipment/ledgers")
public class EquipmentLedgerController {

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
     * 创建设备台账。
     *
     * @param reqVO 创建请求
     * @return 新设备主键 id
     */
    @PostMapping
    public CommonResult<Long> createEquipmentLedger(@Valid @RequestBody EquipmentLedgerSaveReqVO reqVO) {
        return CommonResult.success(ledgerService.createEquipmentLedger(reqVO));
    }

    /**
     * 修改设备台账。
     *
     * @param id    设备主键
     * @param reqVO 修改请求
     * @return 空数据成功响应
     */
    @PutMapping("/{id}")
    public CommonResult<Void> updateEquipmentLedger(@PathVariable("id") @Positive Long id,
                                                     @Valid @RequestBody EquipmentLedgerSaveReqVO reqVO) {
        ledgerService.updateEquipmentLedger(id, reqVO);
        return CommonResult.success(null);
    }

    /**
     * 删除设备台账(逻辑删除)。
     *
     * @param id 设备主键
     * @return 空数据成功响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteEquipmentLedger(@PathVariable("id") @Positive Long id) {
        ledgerService.deleteEquipmentLedger(id);
        return CommonResult.success(null);
    }

    /**
     * 查询设备台账详情。
     *
     * @param id 设备主键
     * @return 设备台账详情
     */
    @GetMapping("/{id}")
    public CommonResult<EquipmentLedgerRespVO> getEquipmentLedger(@PathVariable("id") @Positive Long id) {
        return CommonResult.success(ledgerService.getEquipmentLedger(id));
    }

    /**
     * 分页查询设备台账列表。
     *
     * @param reqVO 分页筛选条件，GET 查询参数绑定
     * @return 分页结果
     */
    @GetMapping("/page")
    public CommonResult<PageResult<EquipmentLedgerRespVO>> getEquipmentLedgerPage(
            @Valid EquipmentLedgerPageReqVO reqVO) {
        return CommonResult.success(ledgerService.getEquipmentLedgerPage(reqVO));
    }
}
