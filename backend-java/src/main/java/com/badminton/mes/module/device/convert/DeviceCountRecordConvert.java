package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountRecordRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;
import com.badminton.mes.module.device.dal.entity.DeviceCountRecordEntity;

/**
 * 设备计数记录显式转换器。
 *
 * <p>计数记录在接收时复制设备编码、采集点、工序和原始报文等关键信息，形成不可变历史快照。
 * 因此设备台账或接入配置后续发生调整，也不会改变既有计数记录所表达的采集现场。</p>
 */
public final class DeviceCountRecordConvert {

    /**
     * 根据上报请求、接入配置和幂等键构造待落库的计数事实快照。
     *
     * <p>设备标识和采集点来自当前配置关系并以快照字段保存；有效增量、匹配状态和上报状态
     * 依赖 Service 的业务判定，因此不在本转换方法内赋值。</p>
     */
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

    /**
     * 将计数实体转换为完整详情响应快照。
     *
     * <p>响应优先使用记录内固化的设备编码和采集点，而非回查当前配置，确保历史展示稳定。</p>
     */
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

    /** 批量生成计数详情快照，并保持输入列表的原有顺序。 */
    public static List<DeviceCountRecordRespVO> toRespVOList(List<DeviceCountRecordEntity> entities) {
        return entities.stream().map(DeviceCountRecordConvert::toRespVO).toList();
    }

    /**
     * 将刚完成的计数判定转换为设备上报确认快照。
     *
     * <p>该精简响应同时回传落库主键、有效增量、后续匹配与报工状态以及异常提示，
     * 不再次读取数据库，因而与当前事务中的判定结果保持一致。</p>
     */
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

    /** 工具类不保存状态，禁止实例化。 */
    private DeviceCountRecordConvert() {
    }
}
