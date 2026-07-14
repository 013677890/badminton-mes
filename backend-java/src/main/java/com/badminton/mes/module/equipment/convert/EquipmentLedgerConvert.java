package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentLedgerSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentLedgerEntity;

/**
 * 设备台账 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值，避免反射拷贝隐藏字段差异。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentLedgerConvert {

    /**
     * 保存请求 VO 转实体。
     *
     * @param reqVO 保存请求 VO
     * @return 设备台账实体
     */
    public static EquipmentLedgerEntity toEntity(EquipmentLedgerSaveReqVO reqVO) {
        EquipmentLedgerEntity equipmentLedger = new EquipmentLedgerEntity();
        equipmentLedger.setEquipmentCode(reqVO.getEquipmentCode());
        equipmentLedger.setEquipmentName(reqVO.getEquipmentName());
        equipmentLedger.setCategoryId(reqVO.getCategoryId());
        equipmentLedger.setManufacturerId(reqVO.getManufacturerId());
        equipmentLedger.setEquipmentModel(reqVO.getEquipmentModel());
        equipmentLedger.setSerialNumber(reqVO.getSerialNumber());
        equipmentLedger.setWorkshopId(reqVO.getWorkshopId());
        equipmentLedger.setProductionLineId(reqVO.getProductionLineId());
        equipmentLedger.setInstallationLocation(reqVO.getInstallationLocation());
        equipmentLedger.setPurchaseDate(reqVO.getPurchaseDate());
        equipmentLedger.setCommissioningDate(reqVO.getCommissioningDate());
        equipmentLedger.setEquipmentStatus(reqVO.getEquipmentStatus());
        equipmentLedger.setResponsiblePerson(reqVO.getResponsiblePerson());
        equipmentLedger.setRemark(reqVO.getRemark());
        equipmentLedger.setStatus(reqVO.getStatus());
        return equipmentLedger;
    }

    /**
     * 实体转响应 VO。
     *
     * @param equipmentLedger 设备台账实体
     * @return 响应 VO
     */
    public static EquipmentLedgerRespVO toRespVO(EquipmentLedgerEntity equipmentLedger) {
        EquipmentLedgerRespVO respVO = new EquipmentLedgerRespVO();
        respVO.setId(equipmentLedger.getId());
        respVO.setEquipmentCode(equipmentLedger.getEquipmentCode());
        respVO.setEquipmentName(equipmentLedger.getEquipmentName());
        respVO.setCategoryId(equipmentLedger.getCategoryId());
        respVO.setManufacturerId(equipmentLedger.getManufacturerId());
        respVO.setEquipmentModel(equipmentLedger.getEquipmentModel());
        respVO.setSerialNumber(equipmentLedger.getSerialNumber());
        respVO.setWorkshopId(equipmentLedger.getWorkshopId());
        respVO.setProductionLineId(equipmentLedger.getProductionLineId());
        respVO.setInstallationLocation(equipmentLedger.getInstallationLocation());
        respVO.setPurchaseDate(equipmentLedger.getPurchaseDate());
        respVO.setCommissioningDate(equipmentLedger.getCommissioningDate());
        respVO.setEquipmentStatus(equipmentLedger.getEquipmentStatus());
        respVO.setResponsiblePerson(equipmentLedger.getResponsiblePerson());
        respVO.setRemark(equipmentLedger.getRemark());
        respVO.setStatus(equipmentLedger.getStatus());
        respVO.setCreateTime(equipmentLedger.getCreateTime());
        respVO.setUpdateTime(equipmentLedger.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 设备台账实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<EquipmentLedgerRespVO> toRespVOList(List<EquipmentLedgerEntity> list) {
        return list.stream().map(EquipmentLedgerConvert::toRespVO).toList();
    }

    private EquipmentLedgerConvert() {
    }
}
