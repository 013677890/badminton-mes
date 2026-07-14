package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountRecordRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;

/** 设备计数记录显式转换器。 */
public final class DeviceCountRecordConvert {

    public static DeviceCountRecordEntity toEntity(DeviceCountReportReqVO request,
                                                   DeviceAccessConfigEntity config,
                                                   String deduplicationKey) {
        DeviceCountRecordEntity entity = new DeviceCountRecordEntity();
        entity.setAccessConfigId(config.getId());
        entity.setEquipmentId(config.getEquipmentId());
        entity.setEquipmentCodeSnapshot(request.getEquipmentCode());
        entity.setCollectionPointCodeSnapshot(config.getCollectionPointCode());
        entity.setCollectedAt(request.getCollectedAt());
        entity.setSerialNumber(request.getSerialNumber());
        entity.setRawCount(request.getCountValue());
        entity.setRuntimeStatus(request.getRuntimeStatus());
        entity.setFaultStatus(request.getFaultStatus());
        entity.setProcessId(config.getProcessId());
        entity.setDeduplicationKey(deduplicationKey);
        entity.setRawPayload(request.getRawPayload());
        return entity;
    }

    public static DeviceCountRecordRespVO toRespVO(DeviceCountRecordEntity entity) {
        DeviceCountRecordRespVO response = new DeviceCountRecordRespVO();
        response.setId(entity.getId());
        response.setAccessConfigId(entity.getAccessConfigId());
        response.setEquipmentId(entity.getEquipmentId());
        response.setEquipmentCode(entity.getEquipmentCodeSnapshot());
        response.setCollectionPointCode(entity.getCollectionPointCodeSnapshot());
        response.setCollectedAt(entity.getCollectedAt());
        response.setSerialNumber(entity.getSerialNumber());
        response.setRawCount(entity.getRawCount());
        response.setIncrementCount(entity.getIncrementCount());
        response.setRuntimeStatus(entity.getRuntimeStatus());
        response.setFaultStatus(entity.getFaultStatus());
        response.setProductionTaskId(entity.getProductionTaskId());
        response.setProcessId(entity.getProcessId());
        response.setMatchStatus(entity.getMatchStatus());
        response.setReportStatus(entity.getReportStatus());
        response.setRawPayload(entity.getRawPayload());
        response.setCreateTime(entity.getCreateTime());
        return response;
    }

    public static List<DeviceCountRecordRespVO> toRespVOList(List<DeviceCountRecordEntity> entities) {
        return entities.stream().map(DeviceCountRecordConvert::toRespVO).toList();
    }

    public static DeviceCountReportRespVO toReportRespVO(DeviceCountRecordEntity entity,
                                                         String exceptionType,
                                                         String processingMessage) {
        DeviceCountReportRespVO response = new DeviceCountReportRespVO();
        response.setCountRecordId(entity.getId());
        response.setIncrementCount(entity.getIncrementCount());
        response.setMatchStatus(entity.getMatchStatus());
        response.setReportStatus(entity.getReportStatus());
        response.setExceptionType(exceptionType);
        response.setProcessingMessage(processingMessage);
        return response;
    }

    private DeviceCountRecordConvert() {
    }
}
