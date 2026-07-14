package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;

/** 设备计数异常显式转换器。 */
public final class DeviceCountExceptionConvert {

    public static DeviceCountExceptionRespVO toRespVO(DeviceCountExceptionEntity entity) {
        DeviceCountExceptionRespVO response = new DeviceCountExceptionRespVO();
        response.setId(entity.getId());
        response.setCountRecordId(entity.getCountRecordId());
        response.setAccessConfigId(entity.getAccessConfigId());
        response.setEquipmentId(entity.getEquipmentId());
        response.setExceptionType(entity.getExceptionType());
        response.setExceptionReason(entity.getExceptionReason());
        response.setProcessingStatus(entity.getProcessingStatus());
        response.setProcessedBy(entity.getProcessedBy());
        response.setProcessedAt(entity.getProcessedAt());
        response.setProcessingResult(entity.getProcessingResult());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    public static List<DeviceCountExceptionRespVO> toRespVOList(List<DeviceCountExceptionEntity> entities) {
        return entities.stream().map(DeviceCountExceptionConvert::toRespVO).toList();
    }

    private DeviceCountExceptionConvert() {
    }
}
