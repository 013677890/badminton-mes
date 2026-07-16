package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCommissioningRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;
import com.badminton.mes.module.device.dal.entity.DeviceCommissioningRecordEntity;

/**
 * 设备联调记录显式转换器。
 *
 * <p>联调实体固化测试时刻、通信与格式校验结论、总结果和样例报文，形成一次联调的事实快照；
 * 测试人及创建审计字段由 Service 根据当前操作上下文补充。</p>
 */
public final class DeviceCommissioningConvert {

    /**
     * 将联调请求转换为待落库的历史记录快照。
     *
     * <p>这里只复制请求携带的测试事实，不修改接入配置的当前联调状态。</p>
     */
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

    /**
     * 将联调记录实体转换为只读响应快照。
     *
     * <p>响应保留测试人与创建时间，便于按当次事实审计，而非读取接入配置的当前状态。</p>
     */
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

    /** 批量生成联调历史快照，并保持输入列表的原有顺序。 */
    public static List<DeviceCommissioningRespVO> toRespVOList(List<DeviceCommissioningRecordEntity> entities) {
        return entities.stream().map(DeviceCommissioningConvert::toRespVO).toList();
    }

    /** 工具类不保存状态，禁止实例化。 */
    private DeviceCommissioningConvert() {
    }
}
