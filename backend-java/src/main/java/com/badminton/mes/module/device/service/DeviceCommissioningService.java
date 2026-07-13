package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCommissioningSaveReqVO;

/** 设备联调记录 Service。 */
public interface DeviceCommissioningService {

    Long createCommissioningRecord(DeviceCommissioningSaveReqVO request);

    DeviceCommissioningRespVO getCommissioningRecord(Long id);

    PageResult<DeviceCommissioningRespVO> getCommissioningRecordPage(DeviceCommissioningPageReqVO request);
}
