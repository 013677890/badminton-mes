package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;

/**
 * 设备制造商 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏，
 * 也避免反射拷贝的性能损耗与浅拷贝陷阱。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentManufacturerConvert {

    /**
     * 保存请求 VO 转实体，创建与修改共用。
     *
     * <p>只搬运业务字段。创建人、创建时间等由 Service 按业务规则另行设置。
     *
     * @param reqVO 保存请求 VO
     * @return 设备制造商实体
     */
    public static EquipmentManufacturerEntity toEntity(EquipmentManufacturerSaveReqVO reqVO) {
        EquipmentManufacturerEntity manufacturer = new EquipmentManufacturerEntity();
        manufacturer.setManufacturerCode(reqVO.getManufacturerCode());
        manufacturer.setManufacturerName(reqVO.getManufacturerName());
        manufacturer.setContactPerson(reqVO.getContactPerson());
        manufacturer.setContactPhone(reqVO.getContactPhone());
        manufacturer.setContactEmail(reqVO.getContactEmail());
        manufacturer.setAddress(reqVO.getAddress());
        manufacturer.setWebsite(reqVO.getWebsite());
        manufacturer.setRemark(reqVO.getRemark());
        manufacturer.setStatus(reqVO.getStatus());
        return manufacturer;
    }

    /**
     * 实体转响应 VO。
     *
     * @param manufacturer 设备制造商实体
     * @return 响应 VO
     */
    public static EquipmentManufacturerRespVO toRespVO(EquipmentManufacturerEntity manufacturer) {
        EquipmentManufacturerRespVO respVO = new EquipmentManufacturerRespVO();
        respVO.setId(manufacturer.getId());
        respVO.setManufacturerCode(manufacturer.getManufacturerCode());
        respVO.setManufacturerName(manufacturer.getManufacturerName());
        respVO.setContactPerson(manufacturer.getContactPerson());
        respVO.setContactPhone(manufacturer.getContactPhone());
        respVO.setContactEmail(manufacturer.getContactEmail());
        respVO.setAddress(manufacturer.getAddress());
        respVO.setWebsite(manufacturer.getWebsite());
        respVO.setRemark(manufacturer.getRemark());
        respVO.setStatus(manufacturer.getStatus());
        respVO.setCreateTime(manufacturer.getCreateTime());
        respVO.setUpdateTime(manufacturer.getUpdateTime());
        return respVO;
    }

    /**
     * 实体列表转响应 VO 列表。
     *
     * @param list 设备制造商实体列表
     * @return 响应 VO 列表；入参为空集合时返回空集合
     */
    public static List<EquipmentManufacturerRespVO> toRespVOList(List<EquipmentManufacturerEntity> list) {
        return list.stream().map(EquipmentManufacturerConvert::toRespVO).toList();
    }

    private EquipmentManufacturerConvert() {
    }
}
