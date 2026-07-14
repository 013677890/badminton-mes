package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCommissioningRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCommissioningRecordEntity;

/** 设备联调记录显式转换器。 */
public final class DeviceCommissioningConvert {

    public static DeviceCommissioningRecordEntity toEntity(DeviceCommissioningSaveReqVO request) {
        DeviceCommissioningRecordEntity entity = new DeviceCommissioningRecordEntity();
        entity.setAccessConfigId(request.getAccessConfigId());
        entity.setTestTime(request.getTestTime());
        entity.setCommunicationResult(request.getCommunicationResult());
        entity.setDataFormatResult(request.getDataFormatResult());
        entity.setTestResult(request.getTestResult());
        entity.setIssueDescription(request.getIssueDescription());
        entity.setSamplePayload(request.getSamplePayload());
        return entity;
    }

    public static DeviceCommissioningRespVO toRespVO(DeviceCommissioningRecordEntity entity) {
        DeviceCommissioningRespVO response = new DeviceCommissioningRespVO();
        response.setId(entity.getId());
        response.setAccessConfigId(entity.getAccessConfigId());
        response.setTestTime(entity.getTestTime());
        response.setTesterUserId(entity.getTesterUserId());
        response.setCommunicationResult(entity.getCommunicationResult());
        response.setDataFormatResult(entity.getDataFormatResult());
        response.setTestResult(entity.getTestResult());
        response.setIssueDescription(entity.getIssueDescription());
        response.setSamplePayload(entity.getSamplePayload());
        response.setCreateTime(entity.getCreateTime());
        return response;
    }

    public static List<DeviceCommissioningRespVO> toRespVOList(List<DeviceCommissioningRecordEntity> entities) {
        return entities.stream().map(DeviceCommissioningConvert::toRespVO).toList();
    }

    private DeviceCommissioningConvert() {
    }
}
