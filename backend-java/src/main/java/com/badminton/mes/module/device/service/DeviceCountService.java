package com.badminton.mes.module.device.service;

import com.badminton.mes.common.core.PageResult;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionResolveReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountExceptionRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordPageReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountRecordRespVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportReqVO;
import com.badminton.mes.module.device.controller.vo.DeviceCountReportRespVO;

/**
 * 设备计数接入及异常处理 Service。
 *
 * <p>计数上报通常由设备接入接口调用，查询和异常处理由对应 Controller 调用；
 * 实现类还负责幂等、数量校验、异常记录和与生产报工的联动。
 *
 * @author MES 开发组
 * @date 2026/07/16
 */
public interface DeviceCountService {

    /** 接收一次设备计数上报，校验接入配置并返回当前计数处理结果。 */
    DeviceCountReportRespVO reportCount(DeviceCountReportReqVO request);

    /** 查询单条设备计数记录。 */
    DeviceCountRecordRespVO getCountRecord(Long id);

    /** 分页查询设备计数记录。 */
    PageResult<DeviceCountRecordRespVO> getCountRecordPage(DeviceCountRecordPageReqVO request);

    /** 查询单条计数异常及其处理状态。 */
    DeviceCountExceptionRespVO getCountException(Long id);

    /** 分页查询待处理或已处理的计数异常。 */
    PageResult<DeviceCountExceptionRespVO> getCountExceptionPage(DeviceCountExceptionPageReqVO request);

    /** 处理计数异常，并记录处理人、处理结果和必要的报工修正。 */
    void processCountException(Long id, DeviceCountExceptionResolveReqVO request);
}
