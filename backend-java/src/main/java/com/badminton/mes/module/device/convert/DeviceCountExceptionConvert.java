package com.badminton.mes.module.device.convert;

import java.util.List;

import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.device.dal.entity.DeviceCountExceptionEntity;

/**
 * 设备计数异常显式转换器。
 *
 * <p>异常响应是异常识别时的关联信息与当前处置进度的组合快照，
 * 既保留计数记录、配置和设备关联，也完整呈现处理人、处理时间及处理结论。</p>
 */
public final class DeviceCountExceptionConvert {

    /**
     * 将异常实体转换为详情响应快照。
     *
     * <p>转换只复制已落库事实，不重新计算异常类型，也不触发任何处置状态变化。</p>
     */
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

    /** 批量生成异常及处置状态快照，并保持输入列表的原有顺序。 */
    public static List<DeviceCountExceptionRespVO> toRespVOList(List<DeviceCountExceptionEntity> entities) {
        return entities.stream().map(DeviceCountExceptionConvert::toRespVO).toList();
    }

    /** 工具类不保存状态，禁止实例化。 */
    private DeviceCountExceptionConvert() {
    }
}
