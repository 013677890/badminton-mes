package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionResolveReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;

/** 设备计数接入及异常处理 Service。 */
public interface DeviceCountService {

    DeviceCountReportRespVO reportCount(DeviceCountReportReqVO request);

    DeviceCountRecordRespVO getCountRecord(Long id);

    PageResult<DeviceCountRecordRespVO> getCountRecordPage(DeviceCountRecordPageReqVO request);

    DeviceCountExceptionRespVO getCountException(Long id);

    PageResult<DeviceCountExceptionRespVO> getCountExceptionPage(DeviceCountExceptionPageReqVO request);

    void processCountException(Long id, DeviceCountExceptionResolveReqVO request);
}
