package com.badminton.mes.module.equipment.convert;

import java.util.List;

import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerRespVO;
import com.badminton.mes.module.equipment.controller.vo.EquipmentManufacturerSaveReqVO;
import com.badminton.mes.module.equipment.dal.entity.EquipmentManufacturerEntity;

/**
 * 设备制造商 VO 与实体的转换器。
 *
 * <p>采用显式逐字段赋值：字段对应关系一目了然，编译期即可发现改名遗漏，
 * 也避免反射拷贝的性能损耗与浅拷贝陷阱。转换层只定义制造商业务字段与接口字段的边界，不负责
 * 编码唯一性、设备引用保护、逻辑删除改名或缓存处理，这些规则必须由事务内的 Service 编排。
 *
 * @author 角色C
 * @date 2026/07/09
 */
public final class EquipmentManufacturerConvert {

    /**
     * 保存请求 VO 转实体，创建与修改共用。
     *
     * <p>只搬运编码、名称、联系方式、地址和启停状态等可维护字段。主键、创建人、时间戳及逻辑
     * 删除标记不从请求产生，防止客户端伪造审计快照；创建默认值由 Service 另行设置。
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
     * <p>输出制造商当前业务信息和创建、更新时间，形成可缓存的详情快照；逻辑删除标记及审计
     * 操作者不属于对外模型，避免缓存内容携带持久层控制语义。
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

    /** 工具类仅提供无状态字段转换，禁止实例化。 */
    private EquipmentManufacturerConvert() {
    }
}
