package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceAccessConfigSaveReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceAccessConfigEntity;

/**
 * 设备接入配置显式转换器。
 *
 * <p>转换过程逐字段列出边界，避免自动映射误把联调状态、数据来源、通信时间等
 * 服务端维护字段当作客户端可编辑字段。响应转换生成与持久化实体解耦的配置快照。</p>
 */
public final class DeviceAccessConfigConvert {

    /**
     * 将创建请求转换为仅包含客户端可编辑字段的新实体。
     *
     * <p>接入来源、默认模式、联调状态、启用状态和审计字段由 Service 随后统一补齐。</p>
     */
    public static DeviceAccessConfigEntity toEntity(DeviceAccessConfigSaveReqVO request) {
        DeviceAccessConfigEntity entity = new DeviceAccessConfigEntity();
        copyEditableFields(request, entity);
        return entity;
    }

    /**
     * 把请求中的可编辑字段覆盖到实体，保留服务端维护的生命周期和通信状态。
     *
     * <p>该方法同时服务创建和更新，因此只表达字段复制，不负责默认值、校验或持久化。</p>
     */
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

    /**
     * 将接入配置实体转换为详情响应快照。
     *
     * <p>快照同时包含业务配置、当前联调与启用状态、最近通信时间及审计时间，
     * 后续实体变更不会反向修改已经返回或写入缓存的响应对象。</p>
     */
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

    /** 批量生成配置响应快照，并保持输入列表的原有顺序。 */
    public static List<DeviceAccessConfigRespVO> toRespVOList(List<DeviceAccessConfigEntity> entities) {
        return entities.stream().map(DeviceAccessConfigConvert::toRespVO).toList();
    }

    /** 工具类不保存状态，禁止实例化。 */
    private DeviceAccessConfigConvert() {
    }
}
