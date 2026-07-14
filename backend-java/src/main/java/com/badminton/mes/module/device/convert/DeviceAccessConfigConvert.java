package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;

/** 设备接入配置显式转换器。 */
public final class DeviceAccessConfigConvert {

    public static DeviceAccessConfigEntity toEntity(DeviceAccessConfigSaveReqVO request) {
        DeviceAccessConfigEntity entity = new DeviceAccessConfigEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    public static void copyEditableFields(DeviceAccessConfigSaveReqVO request,
                                          DeviceAccessConfigEntity entity) {
        entity.setConfigCode(request.getConfigCode());
        entity.setConfigName(request.getConfigName());
        entity.setEquipmentId(request.getEquipmentId());
        entity.setCollectionPointCode(request.getCollectionPointCode());
        entity.setProcessId(request.getProcessId());
        entity.setProductionLineId(request.getProductionLineId());
        entity.setCountMode(request.getCountMode());
        entity.setSpikeThreshold(request.getSpikeThreshold());
        entity.setReportMode(request.getReportMode());
        entity.setEnabledStatus(request.getEnabledStatus());
        entity.setRemark(request.getRemark());
    }

    public static DeviceAccessConfigRespVO toRespVO(DeviceAccessConfigEntity entity) {
        DeviceAccessConfigRespVO response = new DeviceAccessConfigRespVO();
        response.setId(entity.getId());
        response.setConfigCode(entity.getConfigCode());
        response.setConfigName(entity.getConfigName());
        response.setEquipmentId(entity.getEquipmentId());
        response.setCollectionPointCode(entity.getCollectionPointCode());
        response.setProcessId(entity.getProcessId());
        response.setProductionLineId(entity.getProductionLineId());
        response.setDataSource(entity.getDataSource());
        response.setCountMode(entity.getCountMode());
        response.setSpikeThreshold(entity.getSpikeThreshold());
        response.setReportMode(entity.getReportMode());
        response.setCommissioningStatus(entity.getCommissioningStatus());
        response.setEnabledStatus(entity.getEnabledStatus());
        response.setLastCommunicationTime(entity.getLastCommunicationTime());
        response.setRemark(entity.getRemark());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    public static List<DeviceAccessConfigRespVO> toRespVOList(List<DeviceAccessConfigEntity> entities) {
        return entities.stream().map(DeviceAccessConfigConvert::toRespVO).toList();
    }

    private DeviceAccessConfigConvert() {
    }
}
